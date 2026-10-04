package com.expirymate.auth.service;

import com.expirymate.auth.event.UserRegisteredEvent;
import com.expirymate.auth.messaging.UserEventPublisher;
import com.expirymate.auth.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class NotificationClient {

	private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
	private static final String CLASS_NAME = NotificationClient.class.getSimpleName();

	private final RestClient restClient;
	private final String notificationServiceUrl;
	private final UserEventPublisher userEventPublisher;

	public NotificationClient(RestClient.Builder restClientBuilder,
							  @Value("${notification-service.url}") String notificationServiceUrl,
							  UserEventPublisher userEventPublisher) {
		this.restClient = restClientBuilder.build();
		this.notificationServiceUrl = notificationServiceUrl;
		this.userEventPublisher = userEventPublisher;
	}

	public void sendWelcomeEmail(User user) {
		try {
			//        restClient.post().uri(notificationServiceUrl + "/internal/notifications/welcome")
//              .body(Map.of("name", name, "email", email)).retrieve().toBodilessEntity();
			log.info("{} - Publish UserRegisteredEvent for userId: {}, email: {}", CLASS_NAME, user.getId(), user.getEmail());
			UserRegisteredEvent userRegisteredEvent = new UserRegisteredEvent(user.getId(), user.getName(), user.getEmail());

			userEventPublisher.publishUserRegistered(userRegisteredEvent);

		} catch (Exception ex) {
			log.warn("{} - Failed to publish UserRegisteredEvent for userId: {}, email: {}, reason: {}", CLASS_NAME, user.getId(), user.getEmail(), ex.getMessage());
		}
	}
}