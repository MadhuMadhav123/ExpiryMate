package com.expirymate.notification.service;

import com.expirymate.notification.model.NotificationLog;
import com.expirymate.notification.repo.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
	private static final String CLASS_NAME = ReminderService.class.getSimpleName();

	private static final String TYPE_WELCOME = "WELCOME";
	private static final String TYPE_EXPIRY = "EXPIRY";

	private final RestClient restClient;
	private final JavaMailSender mailSender;
	private final NotificationLogRepository notificationLogRepository;
	private final String documentServiceUrl;
	private final String fromAddress;

	public ReminderService(RestClient.Builder restClientBuilder,
						   JavaMailSender mailSender,
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
		log.info("{} - Scheduled reminder scan triggered", CLASS_NAME);

		try {
			int count = runReminderScan();
			log.info("{} - Scheduled reminder scan completed successfully, emailsSent: {}", CLASS_NAME, count);
		} catch (Exception exception) {
			log.error("{} - Scheduled reminder scan failed, reason: {}", CLASS_NAME, exception.getMessage(), exception);
		}
	}

	public int runReminderScan() {
		log.info("{} - Reminder scan started", CLASS_NAME);

		Doc[] documents = restClient.get()
				.uri(documentServiceUrl + "/internal/documents/expiring?days=30")
				.retrieve()
				.body(Doc[].class);

		if (documents == null) {
			log.warn("{} - Reminder scan returned no documents", CLASS_NAME);
			return 0;
		}

		log.info("{} - Reminder scan retrieved {} expiring document(s)", CLASS_NAME, documents.length);

		int sentCount = 0;

		for (Doc document : documents) {

			if (alreadySent(document.id())) {
				log.info("{} - Expiry reminder skipped because it was already sent for documentId: {}", CLASS_NAME, document.id());
				continue;
			}

			if (sendExpiryReminder(document, false)) {
				sentCount++;
			}
		}

		log.info("{} - Reminder scan completed, emailsSent: {}", CLASS_NAME, sentCount);

		return sentCount;
	}

	public boolean sendWelcomeEmail(String name, String email) {
		log.info("{} - Welcome email processing started for email: {}", CLASS_NAME, email);

		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(fromAddress);
			message.setTo(email);
			message.setSubject("ExpiryMate Registration Successful");
			message.setText("Hello " + name + ",\n\n"
					+ "Your registration with ExpiryMate was successful.\n\n"
					+ "You can now add and track important documents and expiry dates.\n\n"
					+ "Regards,\nExpiryMate Team");

			mailSender.send(message);

			saveLog(null, email, "Account registration", TYPE_WELCOME, "SENT", null);

			log.info("{} - Welcome email sent successfully to email: {}", CLASS_NAME, email);

			return true;

		} catch (Exception exception) {
			saveLog(null, email, "Account registration", TYPE_WELCOME, "FAILED", rootMessage(exception));

			log.error("{} - Welcome email failed for email: {}, reason: {}", CLASS_NAME, email, exception.getMessage(), exception);

			return false;
		}
	}

	public void sendImmediateExpiryReminder(Doc document) {
		log.info("{} - Immediate expiry reminder processing started for documentId: {}, email: {}", CLASS_NAME, document.id(), document.ownerEmail());

		boolean sent = sendExpiryReminder(document, true);

		if (sent) {
			log.info("{} - Immediate expiry reminder completed successfully for documentId: {}", CLASS_NAME, document.id());
		} else {
			log.warn("{} - Immediate expiry reminder was not sent for documentId: {}", CLASS_NAME, document.id());
		}
	}

	private boolean sendExpiryReminder(Doc document, boolean force) {

		if (!force && alreadySent(document.id())) {
			log.info("{} - Expiry reminder skipped because it was already sent for documentId: {}", CLASS_NAME, document.id());
			return false;
		}

		try {
			long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), document.expiryDate());

			log.info("{} - Sending expiry reminder for documentId: {}, documentName: {}, daysRemaining: {}", CLASS_NAME, document.id(), document.name(), daysRemaining);

			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(fromAddress);
			message.setTo(document.ownerEmail());
			message.setSubject("ExpiryMate reminder: " + document.name());
			message.setText("Hello,\n\n"
					+ "Your document '" + document.name() + "' (" + document.category()
					+ ") expires on " + document.expiryDate() + ".\n"
					+ (daysRemaining == 0 ? "It expires today.\n" : daysRemaining + " day(s) remaining.\n")
					+ "Please renew it in time.\n\nExpiryMate");

			mailSender.send(message);

			saveLog(document.id(), document.ownerEmail(), document.name(), TYPE_EXPIRY, "SENT", null);

			log.info("{} - Expiry reminder sent successfully for documentId: {}, email: {}", CLASS_NAME, document.id(), document.ownerEmail());

			return true;

		} catch (Exception exception) {
			saveLog(document.id(), document.ownerEmail(), document.name(), TYPE_EXPIRY, "FAILED", rootMessage(exception));

			log.error("{} - Expiry reminder failed for documentId: {}, email: {}, reason: {}", CLASS_NAME, document.id(), document.ownerEmail(), exception.getMessage(), exception);

			return false;
		}
	}

	private boolean alreadySent(Long documentId) {
		boolean sent = documentId != null && notificationLogRepository.existsByDocumentIdAndNotificationTypeAndStatus(documentId, TYPE_EXPIRY, "SENT");

		if (sent) {
			log.info("{} - Existing successful expiry notification found for documentId: {}", CLASS_NAME, documentId);
		}

		return sent;
	}

	private void saveLog(Long documentId, String recipient, String documentName, String notificationType, String status, String errorMessage) {
		NotificationLog notificationLog = new NotificationLog();
		notificationLog.setDocumentId(documentId);
		notificationLog.setRecipient(recipient);
		notificationLog.setDocumentName(documentName);
		notificationLog.setSentAt(LocalDateTime.now());
		notificationLog.setStatus(status);
		notificationLog.setNotificationType(notificationType);
		notificationLog.setErrorMessage(errorMessage);

		notificationLogRepository.save(notificationLog);

		log.info("{} - Notification log saved for documentId: {}, type: {}, status: {}", CLASS_NAME, documentId, notificationType, status);
	}

	private String rootMessage(Exception exception) {
		Throwable current = exception;

		while (current.getCause() != null) {
			current = current.getCause();
		}

		String message = current.getMessage();

		return message == null
				? current.getClass().getSimpleName()
				: message.substring(0, Math.min(message.length(), 950));
	}
}