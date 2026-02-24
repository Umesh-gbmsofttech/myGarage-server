package com.gbm.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gbm.app.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true
        WHERE n.user.id = :userId
          AND n.isRead = false
    """)
    int markAllReadByUserId(@Param("userId") Long userId);
}
