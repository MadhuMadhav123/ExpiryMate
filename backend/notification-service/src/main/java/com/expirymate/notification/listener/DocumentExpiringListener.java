package com.expirymate.notification.listener;

import com.expirymate.notification.config.RabbitMqConfig;
import com.expirymate.notification.event.DocumentExpiringEvent;
import com.expirymate.notification.service.ReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentExpiringListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentExpiringListener.class);
    private static final String CLASS_NAME = DocumentExpiringListener.class.getSimpleName();

    private final ReminderService reminderService;

    public DocumentExpiringListener(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @RabbitListener(queues = RabbitMqConfig.EXPIRY_QUEUE)
    public void handleDocumentExpiring(DocumentExpiringEvent event) {

        log.info("{} - DocumentExpiringEvent received for documentId: {}, userId: {}, email: {}", CLASS_NAME, event.id(), event.userId(), event.ownerEmail());

        try {
            ReminderService.Doc document = new ReminderService.Doc(
                    event.id(),
                    event.userId(),
                    event.ownerEmail(),
                    event.name(),
                    event.category(),
                    event.documentNumber(),
                    null,
                    event.expiryDate(),
                    null,
                    "EXPIRING"
            );

            reminderService.sendImmediateExpiryReminder(document);

            log.info("{} - DocumentExpiringEvent processed successfully for documentId: {}", CLASS_NAME, event.id());

        } catch (Exception exception) {
            log.error("{} - Failed to process DocumentExpiringEvent for documentId: {}, reason: {}", CLASS_NAME, event.id(), exception.getMessage(), exception);
            throw exception;
        }
    }
}