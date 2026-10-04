package com.expirymate.notification.controller;

import com.expirymate.notification.model.NotificationLog;
import com.expirymate.notification.repo.NotificationLogRepository;
import com.expirymate.notification.service.ReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
	private static final String CLASS_NAME = NotificationController.class.getSimpleName();

	private final ReminderService reminderService;
	private final NotificationLogRepository notificationLogRepository;

	public NotificationController(ReminderService reminderService,
								  NotificationLogRepository notificationLogRepository) {
		this.reminderService = reminderService;
		this.notificationLogRepository = notificationLogRepository;
	}

	@PostMapping("/run")
	public Map<String, Object> runReminderScan() {
		log.info("{} - Reminder scan API triggered", CLASS_NAME);

		int count = reminderService.runReminderScan();

		log.info("{} - Reminder scan completed successfully, emailsSent: {}", CLASS_NAME, count);

		return Map.of("message", "Reminder scan completed", "emailsSent", count);
	}

	@GetMapping("/logs")
	public List<NotificationLog> logs() {
		log.info("{} - Notification logs API triggered", CLASS_NAME);

		List<NotificationLog> logs = notificationLogRepository.findAll();

		log.info("{} - Notification logs retrieved successfully, count: {}", CLASS_NAME, logs.size());

		return logs;
	}
}