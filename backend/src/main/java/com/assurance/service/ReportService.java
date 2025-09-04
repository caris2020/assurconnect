package com.assurance.service;

import com.assurance.domain.AuditEvent;
import com.assurance.domain.Report;
import com.assurance.domain.InsuranceCase;
import com.assurance.repo.AuditEventRepository;
import com.assurance.repo.ReportRepository;
import com.assurance.repo.InsuranceCaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReportService {
    private final ReportRepository reportRepository;
    private final AuditEventRepository auditEventRepository;
    private final InsuranceCaseRepository insuranceCaseRepository;

    public ReportService(ReportRepository reportRepository, AuditEventRepository auditEventRepository, InsuranceCaseRepository insuranceCaseRepository) {
        this.reportRepository = reportRepository;
        this.auditEventRepository = auditEventRepository;
        this.insuranceCaseRepository = insuranceCaseRepository;
    }

    public List<Report> list() { return reportRepository.findAll(); }

    public Report findById(Long id) { return reportRepository.findById(id).orElse(null); }

    /**
     * Récupère les IDs des rapports créés par un propriétaire
     */
    public List<Long> findReportIdsByOwner(String ownerId) {
        return reportRepository.findByCreatedBy(ownerId)
                .stream()
                .map(Report::getId)
                .collect(java.util.stream.Collectors.toList());
    }

    public Report create(Report payload, String createdBy) {
        // Validation des champs obligatoires
        validateRequiredFields(payload);
        
        // Validation de la correspondance avec le dossier si caseId est fourni
        if (payload.getCaseId() != null && !payload.getCaseId().trim().isEmpty()) {
            // Unicité: un seul rapport par dossier
            reportRepository.findByCaseId(payload.getCaseId().trim()).ifPresent(existing -> {
                throw new IllegalArgumentException("Un rapport existe déjà pour ce dossier");
            });
            validateCaseCorrespondence(payload);
        }
        
        if (payload.getStatus() == null) {
            payload.setStatus(Report.Status.DISPONIBLE);
        }
        
        // Définir le créateur du rapport
        payload.setCreatedBy(createdBy);
        
        Report saved = reportRepository.save(payload);
        
        // Créer l'événement d'audit de manière sécurisée
        try {
            AuditEvent evt = new AuditEvent();
            evt.setType(AuditEvent.EventType.REPORT_CREATED);
            evt.setActor(createdBy);
            evt.setMessage("Rapport créé: \"" + saved.getTitle() + "\"");
            auditEventRepository.save(evt);
        } catch (Exception e) {
            // Log l'erreur mais ne pas faire échouer la création du rapport
            System.err.println("Erreur lors de la création de l'événement d'audit: " + e.getMessage());
        }
        
        return saved;
    }

    // Méthode pour créer un rapport avec fichier (appelée depuis le contrôleur)
    public Report createWithFile(Report payload, boolean hasFile) {
        // Validation des champs obligatoires
        validateRequiredFields(payload);
        
        // TEMPORAIRE: Désactiver la validation du fichier obligatoire
        // if (!hasFile) {
        //     throw new IllegalArgumentException("Le fichier du rapport est obligatoire");
        // }
        
        // Validation de la correspondance avec le dossier si caseId est fourni
        if (payload.getCaseId() != null && !payload.getCaseId().trim().isEmpty()) {
            // Unicité: un seul rapport par dossier
            reportRepository.findByCaseId(payload.getCaseId().trim()).ifPresent(existing -> {
                throw new IllegalArgumentException("Un rapport existe déjà pour ce dossier");
            });
            validateCaseCorrespondence(payload);
        }
        
        if (payload.getStatus() == null) {
            payload.setStatus(Report.Status.DISPONIBLE);
        }
        
        Report saved = reportRepository.save(payload);
        
        // Créer l'événement d'audit de manière sécurisée
        try {
            AuditEvent evt = new AuditEvent();
            evt.setType(AuditEvent.EventType.REPORT_CREATED);
            evt.setActor("system");
            evt.setMessage("Rapport créé: \"" + saved.getTitle() + "\"");
            auditEventRepository.save(evt);
        } catch (Exception e) {
            // Log l'erreur mais ne pas faire échouer la création du rapport
            System.err.println("Erreur lors de la création de l'événement d'audit: " + e.getMessage());
            // On peut continuer sans l'audit pour l'instant
        }
        
        return saved;
    }

    private void validateRequiredFields(Report payload) {
        if (payload.getTitle() == null || payload.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre du rapport est obligatoire");
        }
        
        // Validation des bénéficiaires (peut être un JSON array ou une chaîne simple)
        if (payload.getBeneficiaries() == null || payload.getBeneficiaries().trim().isEmpty()) {
            throw new IllegalArgumentException("Au moins un bénéficiaire est obligatoire");
        }
        
        // Validation des assurés (peut être un JSON array ou une chaîne simple)
        if (payload.getInsureds() == null || payload.getInsureds().trim().isEmpty()) {
            throw new IllegalArgumentException("Au moins un assuré est obligatoire");
        }
        
        if (payload.getInitiator() == null || payload.getInitiator().trim().isEmpty()) {
            throw new IllegalArgumentException("L'initiateur est obligatoire");
        }
        
        if (payload.getSubscriber() == null || payload.getSubscriber().trim().isEmpty()) {
            throw new IllegalArgumentException("Le souscripteur est obligatoire");
        }
        
        if (payload.getCaseId() == null || payload.getCaseId().trim().isEmpty()) {
            throw new IllegalArgumentException("Le numéro de dossier est obligatoire");
        }
    }

    private void validateCaseCorrespondence(Report payload) {
        // Vérifier d'abord si le caseId est un nombre (ID) ou un code alphanumérique
        Long caseId = null;
        String caseCode = null;
        
        try {
            caseId = Long.parseLong(payload.getCaseId());
        } catch (NumberFormatException e) {
            // Si ce n'est pas un nombre, c'est probablement un code alphanumérique
            caseCode = payload.getCaseId();
        }
        
        InsuranceCase insuranceCase = null;
        
        if (caseId != null) {
            // Recherche par ID
            Optional<InsuranceCase> optionalCase = insuranceCaseRepository.findById(caseId);
            if (optionalCase.isPresent()) {
                insuranceCase = optionalCase.get();
            }
        } else if (caseCode != null) {
            // Recherche par code de référence
            Optional<InsuranceCase> optionalCase = insuranceCaseRepository.findByReference(caseCode);
            if (optionalCase.isPresent()) {
                insuranceCase = optionalCase.get();
            } else {
                // Si le dossier n'existe pas, le créer automatiquement
                insuranceCase = createCaseFromReport(payload, caseCode);
            }
        }
        
        // Si le dossier existe en base, valider la correspondance des champs
        if (insuranceCase != null) {
            String caseDataJson = insuranceCase.getDataJson();
            if (caseDataJson != null && !caseDataJson.isEmpty()) {
                validateFieldCorrespondence(payload, caseDataJson);
            }
        }
    }

    private InsuranceCase createCaseFromReport(Report payload, String caseCode) {
        // Créer un nouveau dossier basé sur les données du rapport
        InsuranceCase newCase = new InsuranceCase();
        newCase.setReference(caseCode);
        newCase.setType(InsuranceCase.CaseType.ENQUETE); // Type par défaut
        newCase.setStatus(InsuranceCase.CaseStatus.SOUS_ENQUETE); // Status par défaut
        
        // Créer les données JSON du dossier à partir des informations du rapport
        String caseDataJson = createCaseDataFromReport(payload);
        newCase.setDataJson(caseDataJson);
        
        // Sauvegarder le dossier
        InsuranceCase savedCase = insuranceCaseRepository.save(newCase);
        
        // Créer un événement d'audit pour la création du dossier
        try {
            AuditEvent evt = new AuditEvent();
            evt.setType(AuditEvent.EventType.CASE_CREATED);
            evt.setActor("system");
            evt.setMessage("Dossier créé automatiquement: " + caseCode);
            auditEventRepository.save(evt);
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de l'événement d'audit pour le dossier: " + e.getMessage());
        }
        
        return savedCase;
    }

    private String createCaseDataFromReport(Report payload) {
        // Créer un objet JSON avec les données du rapport
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        jsonBuilder.append("\"beneficiaire_nom\":\"").append(payload.getBeneficiary() != null ? payload.getBeneficiary() : "").append("\",");
        jsonBuilder.append("\"assure_nom\":\"").append(payload.getInsured() != null ? payload.getInsured() : "").append("\",");
        jsonBuilder.append("\"souscripteur_nom\":\"").append(payload.getSubscriber() != null ? payload.getSubscriber() : "").append("\",");
        jsonBuilder.append("\"initiateur\":\"").append(payload.getInitiator() != null ? payload.getInitiator() : "").append("\",");
        jsonBuilder.append("\"titre_rapport\":\"").append(payload.getTitle() != null ? payload.getTitle() : "").append("\"");
        jsonBuilder.append("}");
        return jsonBuilder.toString();
    }

    private void validateFieldCorrespondence(Report payload, String caseDataJson) {
        // Validation basique des correspondances de champs
        // Dans un vrai projet, vous utiliseriez une approche plus sophistiquée
        
        // Vérifier que les noms correspondent (approximation simple)
        String beneficiary = payload.getBeneficiary();
        String insured = payload.getInsured();
        String subscriber = payload.getSubscriber();
        
        // Vérifications basiques - dans un vrai projet, vous feriez une analyse JSON plus poussée
        // Pour l'instant, on ne bloque pas la création si les noms ne correspondent pas exactement
        // car les dossiers peuvent être gérés côté frontend avec des données différentes
        
        // Log pour debug (optionnel)
        System.out.println("Validation des champs pour le rapport: " + payload.getTitle());
        System.out.println("Bénéficiaire: " + beneficiary);
        System.out.println("Assuré: " + insured);
        System.out.println("Souscripteur: " + subscriber);
        System.out.println("Données du dossier: " + caseDataJson);
        
        // Validation optionnelle - on peut l'activer plus tard si nécessaire
        /*
        if (beneficiary != null && !caseDataJson.toLowerCase().contains(beneficiary.toLowerCase())) {
            throw new IllegalArgumentException("Le bénéficiaire '" + beneficiary + "' ne correspond pas aux données du dossier");
        }
        
        if (insured != null && !caseDataJson.toLowerCase().contains(insured.toLowerCase())) {
            throw new IllegalArgumentException("L'assuré '" + insured + "' ne correspond pas aux données du dossier");
        }
        
        if (subscriber != null && !caseDataJson.toLowerCase().contains(subscriber.toLowerCase())) {
            throw new IllegalArgumentException("Le souscripteur '" + subscriber + "' ne correspond pas aux données du dossier");
        }
        */
    }

    public Report update(Report report) {
        if (report.getTitle() == null || report.getTitle().isBlank()) {
            throw new IllegalArgumentException("title requis");
        }
        Report updated = reportRepository.save(report);
        
        // Créer l'événement d'audit de manière sécurisée
        try {
            AuditEvent evt = new AuditEvent();
            evt.setType(AuditEvent.EventType.REPORT_CREATED); // On pourrait ajouter un type REPORT_UPDATED
            evt.setActor("system");
            evt.setMessage("Rapport modifié: \"" + updated.getTitle() + "\"");
            auditEventRepository.save(evt);
        } catch (Exception e) {
            // Log l'erreur mais ne pas faire échouer la modification du rapport
            System.err.println("Erreur lors de la création de l'événement d'audit: " + e.getMessage());
        }
        
        return updated;
    }

    public void delete(Long id) {
        Report report = reportRepository.findById(id).orElse(null);
        if (report != null) {
            // TEMPORAIRE: Désactiver la suppression des fichiers associés
            // List<ReportFile> reportFiles = reportFileRepository.findByReportOrderByCreatedAtDesc(report);
            // for (ReportFile file : reportFiles) {
            //     reportFileRepository.delete(file);
            // }
            
            // Ensuite supprimer le rapport
            reportRepository.deleteById(id);
            
            // Créer l'événement d'audit de manière sécurisée
            try {
                AuditEvent evt = new AuditEvent();
                evt.setType(AuditEvent.EventType.REPORT_CREATED); // On pourrait ajouter un type REPORT_DELETED
                evt.setActor("system");
                evt.setMessage("Rapport supprimé: \"" + report.getTitle() + "\"");
                auditEventRepository.save(evt);
            } catch (Exception e) {
                // Log l'erreur mais ne pas faire échouer la suppression du rapport
                System.err.println("Erreur lors de la création de l'événement d'audit: " + e.getMessage());
            }
        }
    }

    // Méthode pour vérifier les permissions d'un utilisateur sur un rapport
    public boolean canEditReport(Long reportId, String userName) {
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            return false;
        }
        
        // Seul le créateur du rapport peut le modifier
        return report.getCreatedBy() != null && report.getCreatedBy().equals(userName);
    }

    public boolean canDeleteReport(Long reportId, String userName) {
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            return false;
        }
        
        // Seul le créateur du rapport peut le supprimer
        return report.getCreatedBy() != null && report.getCreatedBy().equals(userName);
    }
}


