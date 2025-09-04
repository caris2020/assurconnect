package com.assurance.service;

import com.assurance.domain.Notification;
import com.assurance.repo.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class InAppNotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    


    /**
     * Envoie une notification in-app à un utilisateur
     */
    @Transactional
    public Notification sendNotification(String userId, Map<String, Object> notificationData) {
        try {
            String title = (String) notificationData.get("title");
            String message = (String) notificationData.get("message");
            String typeStr = (String) notificationData.get("type");
            String action = (String) notificationData.get("action");
            String url = (String) notificationData.get("url");
            
            Notification.NotificationType type = Notification.NotificationType.valueOf(typeStr);
            
            Notification notification = new Notification(userId != null ? userId.trim() : null, title, message, type);
            notification.setAction(action);
            notification.setUrl(url);
            
            // Convertir les métadonnées en JSON
            if (notificationData.size() > 5) { // Plus que les champs de base
                Map<String, Object> metadata = notificationData;
                metadata.remove("title");
                metadata.remove("message");
                metadata.remove("type");
                metadata.remove("action");
                metadata.remove("url");
                
                if (!metadata.isEmpty()) {
                    notification.setMetadata(objectMapper.writeValueAsString(metadata));
                }
            }
            
            Notification savedNotification = notificationRepository.save(notification);
            
            System.out.println("🔔 Notification in-app envoyée à l'utilisateur " + userId);
            System.out.println("ID: " + savedNotification.getId());
            System.out.println("Titre: " + title);
            System.out.println("Message: " + message);
            
            return savedNotification;
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de la notification in-app: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Envoie une notification à tous les administrateurs
     */
    public void sendNotificationToAdmins(Map<String, Object> notificationData) {
        // TODO: Implémenter l'envoi aux administrateurs
        // Pour l'instant, on envoie à un utilisateur "admin" par défaut
        sendNotification("admin", notificationData);
    }
    
    /**
     * Envoie une notification à tous les utilisateurs
     */
    public void sendNotificationToAllUsers(Map<String, Object> notificationData) {
        // TODO: Implémenter l'envoi à tous les utilisateurs
        // Pour l'instant, on envoie à un utilisateur "admin" par défaut
        sendNotification("admin", notificationData);
    }
    
    /**
     * Marque une notification comme lue
     */
    @Transactional
    public boolean markAsRead(Long notificationId, String userId) {
        try {
            Notification notification = notificationRepository.findById(notificationId).orElse(null);
            if (notification != null && notification.getUserId() != null && userId != null && notification.getUserId().equalsIgnoreCase(userId)) {
                notification.setRead(true);
                notification.setReadAt(Instant.now());
                notificationRepository.save(notification);
                System.out.println("✅ Notification " + notificationId + " marquée comme lue par " + userId);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Erreur lors du marquage de la notification: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Récupère les notifications d'un utilisateur
     */
    public List<Notification> getNotifications(String userId) {
        return notificationRepository.findActiveByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Récupère les notifications non lues d'un utilisateur
     */
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findUnreadActiveByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Compte les notifications non lues d'un utilisateur
     */
    public long countUnreadNotifications(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }
    
    /**
     * Supprime une notification individuelle
     */
    @Transactional
    public boolean deleteNotification(Long notificationId, String userId) {
        try {
            int updated = notificationRepository.moveOneToTrash(notificationId, userId);
            if (updated > 0) {
                System.out.println("🗑️ Notification " + notificationId + " déplacée dans la corbeille pour l'utilisateur " + userId);
                return true;
            }
            // Fallback: faire l'update via entité si l'UPDATE JPQL n'a pas été appliqué (problème de transaction/proxy)
            Notification n = notificationRepository.findById(notificationId).orElse(null);
            if (n != null && n.getUserId() != null && userId != null && n.getUserId().equalsIgnoreCase(userId)) {
                n.setDeleted(true);
                n.setDeletedAt(Instant.now());
                notificationRepository.save(n);
                System.out.println("🗑️ (fallback) Notification " + notificationId + " déplacée dans la corbeille pour l'utilisateur " + userId);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression de la notification: " + e.getMessage() + " — tentative fallback");
            // Dernier fallback en cas d'exception: effectuer l'opération via entité
            try {
                Notification n = notificationRepository.findById(notificationId).orElse(null);
                if (n != null && n.getUserId() != null && userId != null && n.getUserId().equalsIgnoreCase(userId)) {
                    n.setDeleted(true);
                    n.setDeletedAt(Instant.now());
                    notificationRepository.save(n);
                    System.out.println("🗑️ (fallback-ex) Notification " + notificationId + " déplacée dans la corbeille pour l'utilisateur " + userId);
                    return true;
                }
            } catch (Exception inner) {
                System.err.println("Erreur fallback suppression: " + inner.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Supprime toutes les notifications d'un utilisateur
     */
    @Transactional
    public boolean deleteAllUserNotifications(String userId) {
        try {
            notificationRepository.moveAllToTrash(userId);
            System.out.println("🗑️ Toutes les notifications déplacées dans la corbeille pour l'utilisateur " + userId);
            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression des notifications: " + e.getMessage());
            return false;
        }
    }

    // Corbeille
    public List<Notification> getTrash(String userId) {
        return notificationRepository.findByUserIdAndDeletedTrueOrderByDeletedAtDesc(userId);
    }

    @Transactional
    public boolean restoreOne(Long id, String userId) {
        return notificationRepository.restoreOne(id, userId) > 0;
    }

    @Transactional
    public int restoreAll(String userId) {
        return notificationRepository.restoreAll(userId);
    }
    
    /**
     * Supprime les anciennes notifications (plus de 30 jours)
     */
    public void cleanupOldNotifications() {
        try {
            Instant cutoffDate = Instant.now().minusSeconds(30 * 24 * 60 * 60); // 30 jours
            notificationRepository.deleteOldNotifications(cutoffDate);
            System.out.println("🧹 Nettoyage des anciennes notifications effectué");
        } catch (Exception e) {
            System.err.println("Erreur lors du nettoyage des notifications: " + e.getMessage());
        }
    }
}
