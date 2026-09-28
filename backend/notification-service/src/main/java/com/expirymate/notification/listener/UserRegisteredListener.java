package com.expirymate.notification.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.expirymate.notification.config.RabbitMqConfig;
import com.expirymate.notification.event.UserRegisteredEvent;
import com.expirymate.notification.service.ReminderService;

@Component
public class UserRegisteredListener {
	private final ReminderService reminderService;

	public UserRegisteredListener(ReminderService reminderService) {

		this.reminderService = reminderService;
	}

	@RabbitListener(queues = RabbitMqConfig.WELCOME_QUEUE)
	public void handleUserRegistered(UserRegisteredEvent event) {

		reminderService.sendWelcomeEmail(event.name(), event.email());
	}
}
