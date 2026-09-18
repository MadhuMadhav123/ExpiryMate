package com.expirymate.notification.controller;

import com.expirymate.notification.service.ReminderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {

	private final ReminderService reminderService;

	public InternalNotificationController(ReminderService reminderService) {
		this.reminderService = reminderService;
	}

	@PostMapping("/welcome")
	public ResponseEntity<Map<String, String>> sendWelcome(@RequestBody WelcomeEmailRequest request) {
		if (request.name() == null || request.name().isBlank() || request.email() == null
				|| request.email().isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("message", "Name and email are required"));
		}

		boolean sent = reminderService.sendWelcomeEmail(request.name().trim(), request.email().trim());
		return sent ? ResponseEntity.ok(Map.of("message", "Welcome email sent"))
				: ResponseEntity.status(503)
						.body(Map.of("message", "Email server unavailable; check notification logs"));
	}

	@PostMapping("/expiry")
	public ResponseEntity<Map<String, String>> sendExpiry(@RequestBody ExpiryEmailRequest request) {
		ReminderService.Doc document = new ReminderService.Doc(request.id(), request.userId(), request.ownerEmail(),
				request.name(), request.category(), request.documentNumber(), null, request.expiryDate(), null,
				"EXPIRING");

		boolean sent = reminderService.sendImmediateExpiryReminder(document);
		return sent ? ResponseEntity.ok(Map.of("message", "Expiry reminder sent"))
				: ResponseEntity.status(503)
						.body(Map.of("message", "Email server unavailable; check notification logs"));
	}

	public record WelcomeEmailRequest(String name, String email) {
	}

	public record ExpiryEmailRequest(Long id, Long userId, String ownerEmail, String name, String category,
			String documentNumber, LocalDate expiryDate) {
	}
}
