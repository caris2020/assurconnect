package com.assurance.web;

import com.assurance.domain.InsuranceCase;
// import com.assurance.domain.Report; // TEMPORAIRE: Désactivé
import com.assurance.repo.ReportRepository;
import com.assurance.repo.InsuranceCaseRepository;
import com.assurance.service.CaseService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/cases")
// CORS géré par WebConfig.java
	public class CaseController {
		private final CaseService caseService;
		private final ReportRepository reportRepository;
		private final InsuranceCaseRepository insuranceCaseRepository;

		public CaseController(CaseService caseService, ReportRepository reportRepository, InsuranceCaseRepository insuranceCaseRepository) {
			this.caseService = caseService;
			this.reportRepository = reportRepository;
			this.insuranceCaseRepository = insuranceCaseRepository;
		}

    @GetMapping
    public List<InsuranceCase> list(@RequestParam(required = false) String creator) { 
        if (creator != null && !creator.trim().isEmpty()) {
            return caseService.listByCreator(creator);
        }
        return caseService.list(); 
    }

    @GetMapping("/my-cases")
    public List<InsuranceCase> myCases(@RequestParam String actorName) {
        return caseService.listByCreator(actorName);
    }

    @GetMapping("/{id}/permissions")
    public Map<String, Boolean> getPermissions(@PathVariable Long id, @RequestParam String actorName) {
        Map<String, Boolean> permissions = new HashMap<>();
        permissions.put("canEdit", caseService.canEdit(id, actorName));
        permissions.put("canDelete", caseService.canDelete(id, actorName));
        return permissions;
    }

    @GetMapping("/reference/{reference}")
    public InsuranceCase findByReference(@PathVariable String reference) {
        // Nettoyer la référence en supprimant les espaces
        String cleanReference = reference.trim();
        return insuranceCaseRepository.findByReference(cleanReference)
            .orElseThrow(() -> new IllegalArgumentException("Dossier non trouvé avec la référence: " + cleanReference));
    }

    	@PostMapping
	public InsuranceCase create(@RequestBody InsuranceCase item, @RequestParam String actorName, @RequestParam(required = false) Long reportId) {
		// Validation de l'actorName
		if (actorName == null || actorName.trim().isEmpty()) {
			throw new IllegalArgumentException("Le paramètre actorName est obligatoire et ne peut pas être vide");
		}
		
		// TEMPORAIRE: Désactiver la liaison avec les rapports pour éviter les erreurs LOB
		// if (reportId != null) {
		//     Report report = reportRepository.findById(reportId).orElseThrow();
		//     item.setReport(report);
		// }
		return caseService.create(item, actorName.trim());
	}

    @PutMapping("/{id}")
    public InsuranceCase update(@PathVariable Long id, @RequestBody InsuranceCase updateData, @RequestParam String actorName) {
        InsuranceCase existingCase = insuranceCaseRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Dossier non trouvé avec l'ID: " + id));
        
        // Vérifier que seul le créateur peut modifier le dossier
        if (!existingCase.getCreatedBy().equals(actorName)) {
            throw new IllegalArgumentException("Seul le créateur du dossier peut le modifier");
        }
        
        // Mettre à jour les champs autorisés
        if (updateData.getStatus() != null) {
            existingCase.setStatus(updateData.getStatus());
        }
        if (updateData.getDataJson() != null) {
            existingCase.setDataJson(updateData.getDataJson());
        }
        
        		return insuranceCaseRepository.save(existingCase);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, @RequestParam String actorName) {
        InsuranceCase existingCase = insuranceCaseRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Dossier non trouvé avec l'ID: " + id));
        
        // Vérifier que seul le créateur peut supprimer le dossier
        if (!existingCase.getCreatedBy().equals(actorName)) {
            throw new IllegalArgumentException("Seul le créateur du dossier peut le supprimer");
        }
        
        // Sauvegarder la référence avant la suppression pour la notification
        String caseReference = existingCase.getReference();
        
        		insuranceCaseRepository.delete(existingCase);
		return "Dossier supprimé avec succès";
    }

    @PostMapping("/cleanup-duplicates")
    public String cleanupDuplicates() {
        int deletedCount = caseService.cleanupDuplicateCases();
        return "Nettoyage terminé. " + deletedCount + " dossiers dupliqués supprimés.";
    }
}


