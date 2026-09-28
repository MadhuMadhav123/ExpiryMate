package com.expirymate.auth.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.expirymate.auth.config.RabbitMqConfig;
import com.expirymate.auth.event.UserRegisteredEvent;

@Service
public class UserEventPublisher {
	private final RabbitTemplate rabbitTemplate;

	public UserEventPublisher(RabbitTemplate rabbitTemplate) {

		this.rabbitTemplate = rabbitTemplate;
	}

	public void publishUserRegistered(UserRegisteredEvent event) {

		rabbitTemplate.convertAndSend(
				RabbitMqConfig.EXCHANGE, 
				RabbitMqConfig.USER_REGISTERED_ROUTING_KEY, 
				event);
	}
}
