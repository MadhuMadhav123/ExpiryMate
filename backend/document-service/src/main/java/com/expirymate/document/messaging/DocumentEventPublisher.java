package com.expirymate.document.messaging;

import com.expirymate.document.config.RabbitMqConfig;
import com.expirymate.document.event.DocumentExpiringEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class DocumentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DocumentEventPublisher.class);
    private static final String CLASS_NAME = DocumentEventPublisher.class.getSimpleName();

    private final RabbitTemplate rabbitTemplate;

    public DocumentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishDocumentExpiring(DocumentExpiringEvent event) {

        log.info("{} - Publishing DocumentExpiringEvent for documentId: {}, email: {}", CLASS_NAME, event.id(), event.ownerEmail());

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE,
                RabbitMqConfig.EXPIRY_ROUTING_KEY,
                event
        );

        log.info("{} - DocumentExpiringEvent published successfully for documentId: {}, routingKey: {}", CLASS_NAME, event.id(), RabbitMqConfig.EXPIRY_ROUTING_KEY);
    }
}