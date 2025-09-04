package com.assurance.web;

import com.assurance.domain.Report;
import com.assurance.domain.ReportFile;
import com.assurance.service.ReportService;
import com.assurance.service.FileService;
import com.assurance.service.ReportRequestService;
import com.assurance.service.NotificationService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/download")
// CORS géré par WebConfig.java
public class DownloadController {
    private final ReportService reportService;
    private final FileService fileService;
    private final ReportRequestService reportRequestService;
    private final NotificationService notificationService;

    public DownloadController(ReportService reportService, FileService fileService, 
                            ReportRequestService reportRequestService, NotificationService notificationService) {
        this.reportService = reportService;
        this.fileService = fileService;
        this.reportRequestService = reportRequestService;
        this.notificationService = notificationService;
    }

    // Endpoint sécurisé avec validation du code de validation (nouveau workflow)
    @GetMapping("/{reportId}")
    public ResponseEntity<ByteArrayResource> downloadSecured(
            @PathVariable("reportId") Long reportId,
            @RequestParam("validationCode") String validationCode) {
        
        try {
            // Valider le code de validation et marquer comme téléchargée
            var request = reportRequestService.validateCodeAndDownload(validationCode);
            
            // Vérifier que la demande correspond au bon rapport
            if (!request.getReportId().equals(reportId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ByteArrayResource("Code de validation invalide pour ce rapport".getBytes()));
            }

            // Récupérer le rapport
            Report report = reportService.findById(reportId);
            if (report == null) {
                return ResponseEntity.notFound().build();
            }

            // Récupérer les fichiers du rapport
            List<ReportFile> files = fileService.getReportFiles(reportId);
            if (files.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ByteArrayResource("Aucun fichier trouvé pour ce rapport".getBytes()));
            }

            // Prendre le premier fichier (ou le plus récent)
            ReportFile reportFile = files.get(0);

            // Télécharger le fichier
            byte[] fileContent = fileService.downloadReportFile(reportFile.getId());
            
            ByteArrayResource resource = new ByteArrayResource(fileContent);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + reportFile.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(reportFile.getContentType()))
                    .contentLength(fileContent.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ByteArrayResource(("Erreur lors du téléchargement: " + e.getMessage()).getBytes()));
        }
    }

    // Endpoint de démonstration (désactivable en PROD via variable d'environnement DISABLE_DEMO_DOWNLOAD=true)
    @GetMapping("/demo/{reportId}")
    public ResponseEntity<ByteArrayResource> downloadDemo(
            @PathVariable("reportId") Long reportId) {
        String disableFlag = System.getenv("DISABLE_DEMO_DOWNLOAD");
        if (disableFlag != null && disableFlag.equalsIgnoreCase("true")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ByteArrayResource("Endpoint de démonstration désactivé".getBytes()));
        }
        
        try {
            // Récupérer le rapport
            Report report = reportService.findById(reportId);
            if (report == null) {
                return ResponseEntity.notFound().build();
            }

            // Récupérer les fichiers du rapport
            List<ReportFile> files = fileService.getReportFiles(reportId);
            if (files.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ByteArrayResource("Aucun fichier trouvé pour ce rapport".getBytes()));
            }

            // Prendre le premier fichier (ou le plus récent)
            ReportFile reportFile = files.get(0);

            // Télécharger le fichier
            byte[] fileContent = fileService.downloadReportFile(reportFile.getId());
            
            ByteArrayResource resource = new ByteArrayResource(fileContent);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + reportFile.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(reportFile.getContentType()))
                    .contentLength(fileContent.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ByteArrayResource(("Erreur lors du téléchargement: " + e.getMessage()).getBytes()));
        }
    }
}
