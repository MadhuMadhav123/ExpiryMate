package com.expirymate.document.messaging;

import com.expirymate.document.config.RabbitMqConfig;
import com.expirymate.document.event.DocumentExpiringEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class DocumentEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public DocumentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishDocumentExpiring(DocumentExpiringEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE,
                RabbitMqConfig.EXPIRY_ROUTING_KEY,
                event
        );
    }
}
