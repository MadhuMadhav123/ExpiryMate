package com.expirymate.document.service;

import com.expirymate.document.event.DocumentExpiringEvent;
import com.expirymate.document.messaging.DocumentEventPublisher;
import com.expirymate.document.model.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Map;

@Service
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
    private static final String CLASS_NAME = NotificationClient.class.getSimpleName();

    private final RestClient restClient;
    private final String notificationServiceUrl;
    private final DocumentEventPublisher documentEventPublisher;

    public NotificationClient(RestClient.Builder restClientBuilder,
                              @Value("${notification-service.url}") String notificationServiceUrl,
                              DocumentEventPublisher documentEventPublisher) {

        this.restClient = restClientBuilder.build();
        this.notificationServiceUrl = notificationServiceUrl;
        this.documentEventPublisher = documentEventPublisher;
    }

    public void sendImmediateExpiryReminder(Document document) {

        log.info("{} - Immediate expiry reminder triggered for documentId: {}, userId: {}, email: {}", CLASS_NAME, document.getId(), document.getUserId(), document.getOwnerEmail());

        try {

//            restClient.post().uri(notificationServiceUrl + "/internal/notifications/expiry")
//                    .body(Map.of("id", document.getId(), "userId", document.getUserId(), "ownerEmail",
//                            document.getOwnerEmail(), "name", document.getName(), "category",
//                            document.getCategory().name(), "documentNumber",
//                            document.getDocumentNumber() == null ? "" : document.getDocumentNumber(), "expiryDate",
//                            document.getExpiryDate().toString()))
//                    .retrieve().toBodilessEntity();

            DocumentExpiringEvent documentExpiringEvent = new DocumentExpiringEvent(
                    document.getId(),
                    document.getUserId(),
                    document.getOwnerEmail(),
                    document.getName(),
                    document.getCategory().name(),
                    document.getDocumentNumber() == null ? "" : document.getDocumentNumber(),
                    document.getIssueDate(),
                    document.getExpiryDate(),
                    document.getNotes(),
                    document.getStatus().name()
            );

            documentEventPublisher.publishDocumentExpiring(documentExpiringEvent);

            log.info("{} - Immediate expiry reminder event published successfully for documentId: {}, routing flow: DocumentEventPublisher", CLASS_NAME, document.getId());

        } catch (Exception exception) {

            log.warn("{} - Immediate expiry reminder failed for documentId: {}, userId: {}, email: {}, reason: {}", CLASS_NAME, document.getId(), document.getUserId(), document.getOwnerEmail(), exception.getMessage());
        }
    }
}