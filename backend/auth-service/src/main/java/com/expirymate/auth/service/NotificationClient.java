package com.expirymate.auth.service;

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

	public NotificationClient(RestClient.Builder restClientBuilder,
			@Value("${notification-service.url}") String notificationServiceUrl) {
		this.restClient = restClientBuilder.build();
		this.notificationServiceUrl = notificationServiceUrl;
	}

	public void sendWelcomeEmail(String name, String email) {
		try {
			restClient.post().uri(notificationServiceUrl + "/internal/notifications/welcome")
					.body(Map.of("name", name, "email", email)).retrieve().toBodilessEntity();
		} catch (Exception ex) {
			// Registration must not fail only because email delivery is unavailable.
			log.warn("Welcome email could not be sent to {}: {}", email, ex.getMessage());
		}
	}
}
