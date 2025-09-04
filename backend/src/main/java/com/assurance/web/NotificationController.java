package com.assurance.web;

import com.assurance.domain.Notification;
import com.assurance.service.InAppNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    @Autowired
    private InAppNotificationService notificationService;
    
    /**
     * Récupère toutes les notifications d'un utilisateur
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable String userId) {
        try {
            List<Notification> notifications = notificationService.getNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // DEBUG: créer une notification de test pour un utilisateur
    @PostMapping("/debug/send")
    public ResponseEntity<Map<String, Object>> debugSend(
            @RequestParam String userId,
            @RequestParam(defaultValue = "Test notification") String title,
            @RequestParam(defaultValue = "Message de test") String message,
            @RequestParam(defaultValue = "REPORT_REQUEST_CONFIRMATION") String type
    ) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title);
            payload.put("message", message);
            payload.put("type", type);
            var saved = notificationService.sendNotification(userId, payload);
            return ResponseEntity.ok(Map.of(
                "ok", saved != null,
                "id", saved != null ? saved.getId() : null
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // Corbeille: lister
    @GetMapping("/user/{userId}/trash")
    public ResponseEntity<List<Notification>> getTrash(@PathVariable String userId) {
        try {
            return ResponseEntity.ok(notificationService.getTrash(userId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Récupère les notifications non lues d'un utilisateur
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable String userId) {
        try {
            List<Notification> notifications = notificationService.getUnreadNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Compte les notifications non lues d'un utilisateur
     */
    @GetMapping("/user/{userId}/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String userId) {
        try {
            long count = notificationService.countUnreadNotifications(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Marque une notification comme lue
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, Boolean>> markAsRead(
            @PathVariable Long notificationId,
            @RequestParam String userId) {
        try {
            boolean success = notificationService.markAsRead(notificationId, userId);
            return ResponseEntity.ok(Map.of("success", success));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Marque toutes les notifications d'un utilisateur comme lues
     */
    @PostMapping("/user/{userId}/read-all")
    public ResponseEntity<Map<String, Boolean>> markAllAsRead(@PathVariable String userId) {
        try {
            List<Notification> unreadNotifications = notificationService.getUnreadNotifications(userId);
            for (Notification notification : unreadNotifications) {
                notificationService.markAsRead(notification.getId(), userId);
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Supprime une notification individuelle
     */
    @DeleteMapping("/{notificationId}")
    @Transactional
    public ResponseEntity<Map<String, Boolean>> deleteNotification(
            @PathVariable Long notificationId,
            @RequestParam String userId) {
        try {
            boolean success = notificationService.deleteNotification(notificationId, userId);
            return ResponseEntity.ok(Map.of("success", success));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Corbeille: restaurer une notification
    @PostMapping("/{notificationId}/restore")
    public ResponseEntity<Map<String, Boolean>> restoreNotification(
            @PathVariable Long notificationId,
            @RequestParam String userId) {
        try {
            boolean success = notificationService.restoreOne(notificationId, userId);
            return ResponseEntity.ok(Map.of("success", success));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Corbeille: restaurer toutes les notifications
    @PostMapping("/user/{userId}/restore-all")
    public ResponseEntity<Map<String, Integer>> restoreAll(@PathVariable String userId) {
        try {
            int restored = notificationService.restoreAll(userId);
            return ResponseEntity.ok(Map.of("restored", restored));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Supprime toutes les notifications d'un utilisateur
     */
    @DeleteMapping("/user/{userId}/all")
    @Transactional
    public ResponseEntity<Map<String, Boolean>> deleteAllUserNotifications(@PathVariable String userId) {
        try {
            boolean success = notificationService.deleteAllUserNotifications(userId);
            return ResponseEntity.ok(Map.of("success", success));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Supprime les anciennes notifications
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, String>> cleanupOldNotifications() {
        try {
            notificationService.cleanupOldNotifications();
            return ResponseEntity.ok(Map.of("message", "Nettoyage effectué"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
