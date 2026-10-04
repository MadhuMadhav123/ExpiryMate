package com.expirymate.auth.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.expirymate.auth.config.RabbitMqConfig;
import com.expirymate.auth.event.UserRegisteredEvent;

@Service
public class UserEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);
	private static final String CLASS_NAME = UserEventPublisher.class.getSimpleName();

	private final RabbitTemplate rabbitTemplate;

	public UserEventPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void publishUserRegistered(UserRegisteredEvent event) {

		log.info("{} - Publishing UserRegisteredEvent for userId: {}, email: {}", CLASS_NAME, event.userId(), event.email());

		rabbitTemplate.convertAndSend(
				RabbitMqConfig.EXCHANGE,
				RabbitMqConfig.USER_REGISTERED_ROUTING_KEY,
				event
		);

		log.info("{} - UserRegisteredEvent published successfully for userId: {}, routingKey: {}", CLASS_NAME, event.userId(), RabbitMqConfig.USER_REGISTERED_ROUTING_KEY);
	}
}