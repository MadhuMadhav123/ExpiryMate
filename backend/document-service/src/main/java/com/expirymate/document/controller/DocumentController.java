package com.expirymate.document.controller;

import com.expirymate.document.model.Document;
import com.expirymate.document.model.DocumentCategory;
import com.expirymate.document.model.DocumentStatus;
import com.expirymate.document.repo.DocumentRepository;
import com.expirymate.document.service.FileStorageService;
import com.expirymate.document.service.NotificationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

	private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
	private static final String CLASS_NAME = DocumentController.class.getSimpleName();

	private final DocumentRepository repository;
	private final FileStorageService fileStorageService;
	private final NotificationClient notificationClient;

	public DocumentController(DocumentRepository repository, FileStorageService fileStorageService, NotificationClient notificationClient) {
		this.repository = repository;
		this.fileStorageService = fileStorageService;
		this.notificationClient = notificationClient;
	}

	@GetMapping
	public List<Document> all(Authentication authentication) {
		Long userId = userId(authentication);
		log.info("{} - Get all documents API triggered for userId: {}", CLASS_NAME, userId);

		List<Document> documents = repository.findByUserIdOrderByExpiryDateAsc(userId);

		log.info("{} - Get all documents successful for userId: {}, documentCount: {}", CLASS_NAME, userId, documents.size());
		return documents;
	}

	@GetMapping("/{id}")
	public ResponseEntity<Document> one(@PathVariable Long id, Authentication authentication) {
		Long userId = userId(authentication);
		log.info("{} - Get document API triggered for documentId: {}, userId: {}", CLASS_NAME, id, userId);

		return ownedDocument(id, authentication)
				.map(document -> {
					log.info("{} - Get document successful for documentId: {}, userId: {}", CLASS_NAME, id, userId);
					return ResponseEntity.ok(document);
				})
				.orElseGet(() -> {
					log.warn("{} - Document not found or unauthorized for documentId: {}, userId: {}", CLASS_NAME, id, userId);
					return ResponseEntity.notFound().build();
				});
	}

	@PostMapping
	public ResponseEntity<?> add(@RequestBody Document document, Authentication authentication) {
		Long userId = userId(authentication);
		log.info("{} - Add document API triggered for name: {}, userId: {}", CLASS_NAME, document.getName(), userId);

		String validationMessage = validateDocument(document, userId, null);

		if (validationMessage != null) {
			log.warn("{} - Add document validation failed for userId: {}, reason: {}", CLASS_NAME, userId, validationMessage);
			return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
		}

		normalize(document);
		document.setId(null);
		document.setUserId(userId);
		document.setOwnerEmail(authentication.getName());

		Document saved = repository.save(document);

		log.info("{} - Document created successfully for documentId: {}, name: {}, userId: {}", CLASS_NAME, saved.getId(), saved.getName(), userId);

		if (LocalDate.now().equals(saved.getExpiryDate())) {
			log.info("{} - Document expires today. Triggering immediate reminder for documentId: {}", CLASS_NAME, saved.getId());
			notificationClient.sendImmediateExpiryReminder(saved);
		}

		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	@PutMapping("/{id}")
	public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Document incoming, Authentication authentication) {
		Long userId = userId(authentication);
		log.info("{} - Update document API triggered for documentId: {}, name: {}, userId: {}", CLASS_NAME, id, incoming.getName(), userId);

		return ownedDocument(id, authentication).<ResponseEntity<?>>map(existing -> {
			String validationMessage = validateDocument(incoming, userId, id);

			if (validationMessage != null) {
				log.warn("{} - Update document validation failed for documentId: {}, userId: {}, reason: {}", CLASS_NAME, id, userId, validationMessage);
				return ResponseEntity.badRequest().body(Map.of("message", validationMessage));
			}

			normalize(incoming);
			existing.setName(incoming.getName());
			existing.setCategory(incoming.getCategory());
			existing.setDocumentNumber(incoming.getDocumentNumber());
			existing.setIssueDate(incoming.getIssueDate());
			existing.setExpiryDate(incoming.getExpiryDate());
			existing.setNotes(incoming.getNotes());

			Document saved = repository.save(existing);

			log.info("{} - Update document successful for documentId: {}, name: {}, userId: {}", CLASS_NAME, saved.getId(), saved.getName(), userId);
			return ResponseEntity.ok(saved);

		}).orElseGet(() -> {
			log.warn("{} - Update document failed. Document not found or unauthorized for documentId: {}, userId: {}", CLASS_NAME, id, userId);
			return ResponseEntity.notFound().build();
		});
	}

	@PostMapping(path = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadFile(@PathVariable Long id, @RequestParam("file") MultipartFile file, Authentication authentication) {
		Long userId = userId(authentication);
		log.info("{} - Upload file API triggered for documentId: {}, fileName: {}, userId: {}", CLASS_NAME, id, file.getOriginalFilename(), userId);

		return ownedDocument(id, authentication).<ResponseEntity<?>>map(document -> {
			try {
				FileStorageService.StoredFile storedFile = fileStorageService.store(file);

				fileStorageService.delete(document.getStoredFileName());

				document.setFileName(storedFile.originalName());
				document.setStoredFileName(storedFile.storedName());
				document.setFileContentType(storedFile.contentType());

				Document saved = repository.save(document);

				log.info("{} - File upload successful for documentId: {}, fileName: {}, userId: {}", CLASS_NAME, id, storedFile.originalName(), userId);
				return ResponseEntity.ok(saved);

			} catch (IllegalArgumentException exception) {
				log.warn("{} - File upload validation failed for documentId: {}, userId: {}, reason: {}", CLASS_NAME, id, userId, exception.getMessage());
				return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));

			} catch (Exception exception) {
				log.error("{} - File upload failed for documentId: {}, userId: {}", CLASS_NAME, id, userId, exception);
				return ResponseEntity.internalServerError().body(Map.of("message", "Unable to store document file"));
			}

		}).orElseGet(() -> {
			log.warn("{} - File upload failed. Document not found or unauthorized for documentId: {}, userId: {}", CLASS_NAME, id, userId);
			return ResponseEntity.notFound().build();
		});
	}

	@GetMapping("/{id}/file")
	public ResponseEntity<?> downloadFile(@PathVariable Long id, Authentication authentication) {
		Long userId = userId(authentication);
		log.info("{} - Download file API triggered for documentId: {}, userId: {}", CLASS_NAME, id, userId);

		return ownedDocument(id, authentication).<ResponseEntity<?>>map(document -> {
			if (!document.isHasFile()) {
				log.warn("{} - Download failed. No file available for documentId: {}, userId: {}", CLASS_NAME, id, userId);
				return ResponseEntity.notFound().build();
			}

			try {
				Resource resource = fileStorageService.load(document.getStoredFileName());

				MediaType mediaType = document.getFileContentType() == null
						? MediaType.APPLICATION_OCTET_STREAM
						: MediaType.parseMediaType(document.getFileContentType());

				log.info("{} - File download successful for documentId: {}, fileName: {}, userId: {}", CLASS_NAME, id, document.getFileName(), userId);

				return ResponseEntity.ok()
						.contentType(mediaType)
						.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(document.getFileName()).build().toString())
						.body(resource);

			} catch (Exception exception) {
				log.error("{} - File download failed for documentId: {}, userId: {}", CLASS_NAME, id, userId, exception);
				return ResponseEntity.notFound().build();
			}

		}).orElseGet(() -> {
			log.warn("{} - Download failed. Document not found or unauthorized for documentId: {}, userId: {}", CLASS_NAME, id, userId);
			return ResponseEntity.notFound().build();
		});
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
		Long userId = userId(authentication);
		log.info("{} - Delete document API triggered for documentId: {}, userId: {}", CLASS_NAME, id, userId);

		return ownedDocument(id, authentication)
				.map(document -> {
					fileStorageService.delete(document.getStoredFileName());
					repository.delete(document);

					log.info("{} - Document deleted successfully for documentId: {}, userId: {}", CLASS_NAME, id, userId);
					return ResponseEntity.noContent().<Void>build();
				})
				.orElseGet(() -> {
					log.warn("{} - Delete failed. Document not found or unauthorized for documentId: {}, userId: {}", CLASS_NAME, id, userId);
					return ResponseEntity.notFound().build();
				});
	}

	@GetMapping("/search")
	public List<Document> search(@RequestParam(defaultValue = "") String name, Authentication authentication) {
		Long userId = userId(authentication);
		log.info("{} - Search documents API triggered for name: {}, userId: {}", CLASS_NAME, name, userId);

		List<Document> documents = repository.findByUserIdAndNameContainingIgnoreCaseOrderByExpiryDateAsc(userId, name);

		log.info("{} - Search documents successful for userId: {}, resultCount: {}", CLASS_NAME, userId, documents.size());
		return documents;
	}

	@GetMapping("/filter")
	public List<Document> filter(@RequestParam(required = false) DocumentCategory category,
								 @RequestParam(required = false) DocumentStatus status,
								 Authentication authentication) {

		Long userId = userId(authentication);
		log.info("{} - Filter documents API triggered for category: {}, status: {}, userId: {}", CLASS_NAME, category, status, userId);

		List<Document> documents = repository.findByUserIdOrderByExpiryDateAsc(userId).stream()
				.filter(document -> category == null || document.getCategory() == category)
				.filter(document -> status == null || document.getStatus() == status)
				.toList();

		log.info("{} - Filter documents successful for userId: {}, resultCount: {}", CLASS_NAME, userId, documents.size());
		return documents;
	}

	@GetMapping("/upcoming")
	public List<Document> upcoming(@RequestParam(defaultValue = "30") int days, Authentication authentication) {
		Long userId = userId(authentication);
		log.info("{} - Upcoming documents API triggered for days: {}, userId: {}", CLASS_NAME, days, userId);

		LocalDate today = LocalDate.now();
		LocalDate endDate = today.plusDays(days);

		List<Document> documents = repository.findByUserIdOrderByExpiryDateAsc(userId).stream()
				.filter(document -> !document.getExpiryDate().isBefore(today))
				.filter(document -> !document.getExpiryDate().isAfter(endDate))
				.toList();

		log.info("{} - Upcoming documents API successful for userId: {}, resultCount: {}", CLASS_NAME, userId, documents.size());
		return documents;
	}

	@GetMapping("/dashboard")
	public Map<String, Long> dashboard(Authentication authentication) {
		Long userId = userId(authentication);
		log.info("{} - Dashboard API triggered for userId: {}", CLASS_NAME, userId);

		Map<DocumentStatus, Long> counts = repository.findByUserIdOrderByExpiryDateAsc(userId).stream()
				.collect(Collectors.groupingBy(Document::getStatus, Collectors.counting()));

		Map<String, Long> dashboard = Map.of(
				"active", counts.getOrDefault(DocumentStatus.ACTIVE, 0L),
				"expiring", counts.getOrDefault(DocumentStatus.EXPIRING, 0L),
				"expired", counts.getOrDefault(DocumentStatus.EXPIRED, 0L),
				"total", counts.values().stream().mapToLong(Long::longValue).sum()
		);

		log.info("{} - Dashboard API successful for userId: {}, totalDocuments: {}", CLASS_NAME, userId, dashboard.get("total"));
		return dashboard;
	}

	private java.util.Optional<Document> ownedDocument(Long id, Authentication authentication) {
		return repository.findById(id).filter(document -> document.getUserId().equals(userId(authentication)));
	}

	private Long userId(Authentication authentication) {
		return (Long) authentication.getDetails();
	}

	private String validateDocument(Document document, Long userId, Long editingId) {

		if (document.getName() == null || document.getName().trim().isEmpty()) {
			return "Document name is required";
		}

		if (document.getName().trim().length() < 2 || document.getName().trim().length() > 120) {
			return "Document name must be between 2 and 120 characters";
		}

		if (document.getDocumentNumber() != null && document.getDocumentNumber().trim().length() > 100) {
			return "Document number must not exceed 100 characters";
		}

		if (document.getNotes() != null && document.getNotes().trim().length() > 1000) {
			return "Notes must not exceed 1000 characters";
		}

		if (document.getCategory() == null) {
			return "Document category is required";
		}

		if (document.getExpiryDate() == null) {
			return "Expiry date is required";
		}

		if (document.getIssueDate() != null && document.getIssueDate().isAfter(document.getExpiryDate())) {
			return "Issue date cannot be after expiry date";
		}

		String name = document.getName().trim();

		boolean duplicateName = editingId == null
				? repository.existsByUserIdAndNameIgnoreCase(userId, name)
				: repository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, editingId);

		if (duplicateName) {
			return "A document with this name already exists";
		}

		String number = normalizeText(document.getDocumentNumber());

		if (number != null) {
			boolean duplicateActiveNumber = repository.findByUserIdOrderByExpiryDateAsc(userId).stream()
					.filter(existing -> editingId == null || !existing.getId().equals(editingId))
					.filter(existing -> existing.getStatus() != DocumentStatus.EXPIRED)
					.map(Document::getDocumentNumber)
					.map(this::normalizeText)
					.anyMatch(existingNumber -> existingNumber != null && existingNumber.equalsIgnoreCase(number));

			if (duplicateActiveNumber) {
				return "This document number is already used by an active or expiring document";
			}
		}

		return null;
	}

	private void normalize(Document document) {
		document.setName(document.getName().trim());
		document.setDocumentNumber(normalizeText(document.getDocumentNumber()));
		document.setNotes(normalizeText(document.getNotes()));
	}

	private String normalizeText(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		return value.trim();
	}
}