package com.expirymate.auth.service;

import com.expirymate.auth.event.UserRegisteredEvent;
import com.expirymate.auth.messaging.UserEventPublisher;
import com.expirymate.auth.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class NotificationClient {

	private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

	private final RestClient restClient;
	private final String notificationServiceUrl;
	private final UserEventPublisher userEventPublisher;

	public NotificationClient(RestClient.Builder restClientBuilder,
			@Value("${notification-service.url}") String notificationServiceUrl,UserEventPublisher userEventPublisher) {
		this.restClient = restClientBuilder.build();
		this.notificationServiceUrl = notificationServiceUrl;
		this.userEventPublisher = userEventPublisher;
	}

	public void sendWelcomeEmail(User user) {
		try {
//			restClient.post().uri(notificationServiceUrl + "/internal/notifications/welcome")
//					.body(Map.of("name", name, "email", email)).retrieve().toBodilessEntity();
			UserRegisteredEvent userRegisteredEvent = new UserRegisteredEvent(user.getId(),user.getName(),user.getEmail());
			userEventPublisher.publishUserRegistered(userRegisteredEvent);
		} catch (Exception ex) {
			// Registration must not fail only because email delivery is unavailable.
			log.warn("Welcome email could not be sent to {}: {}", user.getEmail(), ex.getMessage());
		}
	}
}
