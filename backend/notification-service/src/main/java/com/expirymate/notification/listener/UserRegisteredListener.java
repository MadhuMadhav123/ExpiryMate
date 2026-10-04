package com.expirymate.notification.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.expirymate.notification.config.RabbitMqConfig;
import com.expirymate.notification.event.UserRegisteredEvent;
import com.expirymate.notification.service.ReminderService;

@Component
public class UserRegisteredListener {

	private static final Logger log = LoggerFactory.getLogger(UserRegisteredListener.class);
	private static final String CLASS_NAME = UserRegisteredListener.class.getSimpleName();

	private final ReminderService reminderService;

	public UserRegisteredListener(ReminderService reminderService) {
		this.reminderService = reminderService;
	}

	@RabbitListener(queues = RabbitMqConfig.WELCOME_QUEUE)
	public void handleUserRegistered(UserRegisteredEvent event) {

		log.info("{} - UserRegisteredEvent received for userId: {}, email: {}", CLASS_NAME, event.userId(), event.email());

		try {
			reminderService.sendWelcomeEmail(event.name(), event.email());

			log.info("{} - UserRegisteredEvent processed successfully for userId: {}, email: {}", CLASS_NAME, event.userId(), event.email());

		} catch (Exception exception) {
			log.error("{} - Failed to process UserRegisteredEvent for userId: {}, email: {}, reason: {}", CLASS_NAME, event.userId(), event.email(), exception.getMessage(), exception);
			throw exception;
		}
	}
}