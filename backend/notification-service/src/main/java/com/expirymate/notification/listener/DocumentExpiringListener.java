package com.expirymate.notification.listener;

import com.expirymate.notification.config.RabbitMqConfig;
import com.expirymate.notification.event.DocumentExpiringEvent;
import com.expirymate.notification.service.ReminderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentExpiringListener {
    private final ReminderService reminderService;

    public DocumentExpiringListener(ReminderService reminderService) {

        this.reminderService = reminderService;
    }

    @RabbitListener(queues = RabbitMqConfig.EXPIRY_QUEUE)
    public void handleDocumentExpiring(DocumentExpiringEvent event) {
        ReminderService.Doc document = new ReminderService.Doc(event.id(), event.userId(), event.ownerEmail(),
                event.name(), event.category(), event.documentNumber(), null, event.expiryDate(), null,
                "EXPIRING");
        reminderService.sendImmediateExpiryReminder(document);
    }
}
