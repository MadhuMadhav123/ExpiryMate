package com.expirymate.notification.service;

import com.expirymate.notification.model.NotificationLog;
import com.expirymate.notification.repo.NotificationLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class ReminderService {

	private static final String TYPE_WELCOME = "WELCOME";
	private static final String TYPE_EXPIRY = "EXPIRY";

	private final RestClient restClient;
	private final JavaMailSender mailSender;
	private final NotificationLogRepository notificationLogRepository;
	private final String documentServiceUrl;
	private final String fromAddress;

	public ReminderService(RestClient.Builder restClientBuilder, JavaMailSender mailSender,
			NotificationLogRepository notificationLogRepository,
			@Value("${document-service.url}") String documentServiceUrl,
			@Value("${app.mail.from:reminders@expirymate.local}") String fromAddress) {
		this.restClient = restClientBuilder.build();
		this.mailSender = mailSender;
		this.notificationLogRepository = notificationLogRepository;
		this.documentServiceUrl = documentServiceUrl;
		this.fromAddress = fromAddress;
	}

	public record Doc(Long id, Long userId, String ownerEmail, String name, String category, String documentNumber,
			LocalDate issueDate, LocalDate expiryDate, String notes, String status) {
	}

	@Scheduled(fixedDelayString = "${app.reminders.scan-delay-ms:60000}", initialDelay = 20000)
	public void scheduledReminderScan() {
		runReminderScan();
	}

	public int runReminderScan() {
		Doc[] documents = restClient.get().uri(documentServiceUrl + "/internal/documents/expiring?days=30").retrieve()
				.body(Doc[].class);

		if (documents == null) {
			return 0;
		}

		int sentCount = 0;
		for (Doc document : documents) {
			if (alreadySent(document.id())) {
				continue;
			}
			if (sendExpiryReminder(document, false)) {
				sentCount++;
			}
		}
		return sentCount;
	}

	public boolean sendWelcomeEmail(String name, String email) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(fromAddress);
			message.setTo(email);
			message.setSubject("ExpiryMate Registration Successful");
			message.setText("Hello " + name + ",\n\n" + "Your registration with ExpiryMate was successful.\n\n"
					+ "You can now add and track important documents and expiry dates.\n\n"
					+ "Regards,\nExpiryMate Team");
			mailSender.send(message);
			saveLog(null, email, "Account registration", TYPE_WELCOME, "SENT", null);
			return true;
		} catch (Exception exception) {
			saveLog(null, email, "Account registration", TYPE_WELCOME, "FAILED", rootMessage(exception));
			return false;
		}
	}

	public boolean sendImmediateExpiryReminder(Doc document) {
		return sendExpiryReminder(document, true);
	}

	private boolean sendExpiryReminder(Doc document, boolean force) {
		if (!force && alreadySent(document.id())) {
			return false;
		}

		try {
			long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), document.expiryDate());
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(fromAddress);
			message.setTo(document.ownerEmail());
			message.setSubject("ExpiryMate reminder: " + document.name());
			message.setText("Hello,\n\n" + "Your document '" + document.name() + "' (" + document.category()
					+ ") expires on " + document.expiryDate() + ".\n"
					+ (daysRemaining == 0 ? "It expires today.\n" : daysRemaining + " day(s) remaining.\n")
					+ "Please renew it in time.\n\nExpiryMate");
			mailSender.send(message);
			saveLog(document.id(), document.ownerEmail(), document.name(), TYPE_EXPIRY, "SENT", null);
			return true;
		} catch (Exception exception) {
			saveLog(document.id(), document.ownerEmail(), document.name(), TYPE_EXPIRY, "FAILED",
					rootMessage(exception));
			return false;
		}
	}

	private boolean alreadySent(Long documentId) {
		return documentId != null && notificationLogRepository
				.existsByDocumentIdAndNotificationTypeAndStatus(documentId, TYPE_EXPIRY, "SENT");
	}

	private void saveLog(Long documentId, String recipient, String documentName, String notificationType, String status,
			String errorMessage) {
		NotificationLog log = new NotificationLog();
		log.setDocumentId(documentId);
		log.setRecipient(recipient);
		log.setDocumentName(documentName);
		log.setSentAt(LocalDateTime.now());
		log.setStatus(status);
		log.setNotificationType(notificationType);
		log.setErrorMessage(errorMessage);
		notificationLogRepository.save(log);
	}

	private String rootMessage(Exception exception) {
		Throwable current = exception;
		while (current.getCause() != null) {
			current = current.getCause();
		}
		String message = current.getMessage();
		return message == null ? current.getClass().getSimpleName()
				: message.substring(0, Math.min(message.length(), 950));
	}
}
