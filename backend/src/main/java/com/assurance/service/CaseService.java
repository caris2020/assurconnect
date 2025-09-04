package com.assurance.service;

import com.assurance.domain.AuditEvent;
import com.assurance.domain.InsuranceCase;
import com.assurance.repo.AuditEventRepository;
import com.assurance.repo.InsuranceCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HexFormat;
import java.security.SecureRandom;

@Service
public class CaseService {
    private final InsuranceCaseRepository insuranceCaseRepository;
    private final AuditEventRepository auditEventRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public CaseService(InsuranceCaseRepository insuranceCaseRepository, AuditEventRepository auditEventRepository) {
        this.insuranceCaseRepository = insuranceCaseRepository;
        this.auditEventRepository = auditEventRepository;
    }

    public List<InsuranceCase> list() {
        return insuranceCaseRepository.findAll();
    }

    public List<InsuranceCase> listByCreator(String creatorName) {
        return insuranceCaseRepository.findByCreatedBy(creatorName);
    }

    public boolean canEdit(Long caseId, String actorName) {
        InsuranceCase insuranceCase = insuranceCaseRepository.findById(caseId).orElse(null);
        return insuranceCase != null && insuranceCase.getCreatedBy().equals(actorName);
    }

    public boolean canDelete(Long caseId, String actorName) {
        return canEdit(caseId, actorName);
    }

    	@Transactional
	public InsuranceCase create(InsuranceCase item, String actorName) {
		// Validation de l'actorName
		if (actorName == null || actorName.trim().isEmpty()) {
			throw new IllegalArgumentException("Le nom de l'acteur ne peut pas être null ou vide");
		}
		
		if (item.getReference() == null || item.getReference().isBlank()) {
			item.setReference(generateReference());
		}
		item.setCreatedBy(actorName.trim());
		InsuranceCase saved = insuranceCaseRepository.save(item);

		AuditEvent evt = new AuditEvent();
		evt.setType(AuditEvent.EventType.CASE_CREATED);
		evt.setActor(actorName);
		evt.setMessage("Création dossier (" + item.getType() + ")");
		auditEventRepository.save(evt);
		return saved;
	}

    private String generateReference() {
        byte[] bytes = new byte[8];
        secureRandom.nextBytes(bytes);
        String base36 = Long.toString(Long.parseUnsignedLong(HexFormat.of().formatHex(bytes), 16), 36).toUpperCase();
        base36 = base36.replaceAll("[^A-Z0-9]", "");
        if (base36.length() < 10) base36 = (base36 + "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789").substring(0, 10);
        return base36.substring(0, 10);
    }

    @Transactional
    public int cleanupDuplicateCases() {
        List<InsuranceCase> allCases = insuranceCaseRepository.findAll();
        int deletedCount = 0;
        
        // Grouper les dossiers par contenu (dataJson) et garder seulement le plus récent
        Map<String, List<InsuranceCase>> casesByContent = new HashMap<>();
        
        for (InsuranceCase insuranceCase : allCases) {
            String content = insuranceCase.getDataJson() != null ? insuranceCase.getDataJson() : "";
            casesByContent.computeIfAbsent(content, k -> new ArrayList<>()).add(insuranceCase);
        }
        
        // Pour chaque groupe de dossiers avec le même contenu, garder seulement le plus récent
        for (List<InsuranceCase> duplicates : casesByContent.values()) {
            if (duplicates.size() > 1) {
                // Trier par date de création (le plus récent en premier)
                duplicates.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                
                // Supprimer tous sauf le premier (le plus récent)
                for (int i = 1; i < duplicates.size(); i++) {
                    insuranceCaseRepository.delete(duplicates.get(i));
                    deletedCount++;
                }
            }
        }
        
        return deletedCount;
    }
}


