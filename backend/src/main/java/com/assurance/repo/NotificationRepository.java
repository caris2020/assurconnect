package com.assurance.repo;

import com.assurance.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * Trouve toutes les notifications d'un utilisateur, triées par date de création (plus récentes d'abord)
     */
    @Query("SELECT n FROM Notification n WHERE lower(n.userId) = lower(:userId) AND (n.deleted = false OR n.deleted IS NULL) ORDER BY n.createdAt DESC")
    List<Notification> findActiveByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);
    
    /**
     * Trouve les notifications non lues d'un utilisateur
     */
    @Query("SELECT n FROM Notification n WHERE lower(n.userId) = lower(:userId) AND n.read = false AND (n.deleted = false OR n.deleted IS NULL) ORDER BY n.createdAt DESC")
    List<Notification> findUnreadActiveByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);
    
    /**
     * Compte les notifications non lues d'un utilisateur
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE lower(n.userId) = lower(:userId) AND n.read = false AND (n.deleted = false OR n.deleted IS NULL)")
    long countByUserIdAndReadFalse(@Param("userId") String userId);
    
    /**
     * Trouve les notifications d'un utilisateur par type
     */
    @Query("SELECT n FROM Notification n WHERE lower(n.userId) = lower(:userId) AND n.type = :type AND (n.deleted = false OR n.deleted IS NULL) ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(@Param("userId") String userId, @Param("type") Notification.NotificationType type);
    
    /**
     * Trouve les notifications non lues d'un utilisateur par type
     */
    @Query("SELECT n FROM Notification n WHERE lower(n.userId) = lower(:userId) AND n.type = :type AND n.read = false AND (n.deleted = false OR n.deleted IS NULL) ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndTypeAndReadFalseOrderByCreatedAtDesc(@Param("userId") String userId, @Param("type") Notification.NotificationType type);
    
    /**
     * Supprime toutes les notifications d'un utilisateur
     */
    // Déplacement vers corbeille (suppression logique)
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE Notification n SET n.deleted = true, n.deletedAt = CURRENT_TIMESTAMP WHERE lower(n.userId) = lower(:userId) AND n.deleted = false")
    void moveAllToTrash(@Param("userId") String userId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE Notification n SET n.deleted = true, n.deletedAt = CURRENT_TIMESTAMP WHERE n.id = :id AND lower(n.userId) = lower(:userId) AND n.deleted = false")
    int moveOneToTrash(@Param("id") Long id, @Param("userId") String userId);

    // Restauration depuis corbeille
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE Notification n SET n.deleted = false, n.deletedAt = NULL WHERE n.id = :id AND lower(n.userId) = lower(:userId) AND n.deleted = true")
    int restoreOne(@Param("id") Long id, @Param("userId") String userId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE Notification n SET n.deleted = false, n.deletedAt = NULL WHERE lower(n.userId) = lower(:userId) AND n.deleted = true")
    int restoreAll(@Param("userId") String userId);

    // Liste corbeille
    @Query("SELECT n FROM Notification n WHERE lower(n.userId) = lower(:userId) AND n.deleted = true ORDER BY n.deletedAt DESC")
    List<Notification> findByUserIdAndDeletedTrueOrderByDeletedAtDesc(@Param("userId") String userId);
    
    /**
     * Supprime les anciennes notifications (plus de 30 jours)
     */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate AND n.deleted = true")
    void deleteOldNotifications(@Param("cutoffDate") java.time.Instant cutoffDate);
}
