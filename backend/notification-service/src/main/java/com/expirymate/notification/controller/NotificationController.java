package com.expirymate.notification.controller;

import com.expirymate.notification.model.NotificationLog;
import com.expirymate.notification.repo.NotificationLogRepository;
import com.expirymate.notification.service.ReminderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final ReminderService reminderService;
	private final NotificationLogRepository notificationLogRepository;

	public NotificationController(ReminderService reminderService,
			NotificationLogRepository notificationLogRepository) {
		this.reminderService = reminderService;
		this.notificationLogRepository = notificationLogRepository;
	}

	@PostMapping("/run")
	public Map<String, Object> runReminderScan() {
		int count = reminderService.runReminderScan();
		return Map.of("message", "Reminder scan completed", "emailsSent", count);
	}

	@GetMapping("/logs")
	public List<NotificationLog> logs() {
		return notificationLogRepository.findAll();
	}
}
