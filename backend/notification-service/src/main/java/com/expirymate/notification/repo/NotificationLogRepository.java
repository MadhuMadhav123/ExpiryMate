package com.expirymate.notification.repo;

import com.expirymate.notification.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

	boolean existsByDocumentIdAndNotificationTypeAndStatus(Long documentId, String notificationType, String status);
}
