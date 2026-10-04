package com.expirymate.document.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

	private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
	private static final String CLASS_NAME = FileStorageService.class.getSimpleName();

	private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");

	private final Path storageDirectory;

	public FileStorageService(@Value("${app.storage.path:uploads}") String storagePath) throws IOException {
		this.storageDirectory = Paths.get(storagePath).toAbsolutePath().normalize();
		Files.createDirectories(storageDirectory);
	}

	public StoredFile store(MultipartFile file) throws IOException {

		if (file == null || file.isEmpty()) {
			log.warn("{} - File upload validation failed: no file selected", CLASS_NAME);
			throw new IllegalArgumentException("Please select a document file");
		}

		if (file.getSize() > 10 * 1024 * 1024) {
			log.warn("{} - File upload validation failed for fileName: {}, reason: file size exceeds 10 MB", CLASS_NAME, file.getOriginalFilename());
			throw new IllegalArgumentException("File size must be 10 MB or less");
		}

		String contentType = file.getContentType();

		if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
			log.warn("{} - File upload validation failed for fileName: {}, contentType: {}", CLASS_NAME, file.getOriginalFilename(), contentType);
			throw new IllegalArgumentException("Only PDF, JPG and PNG files are allowed");
		}

		String originalName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
		String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
		String storedName = UUID.randomUUID() + extension;
		Path target = storageDirectory.resolve(storedName).normalize();

		try {
			Files.copy(file.getInputStream(), target);
			log.info("{} - File stored successfully with originalName: {}, storedName: {}", CLASS_NAME, originalName, storedName);
			return new StoredFile(originalName, storedName, contentType);
		} catch (IOException exception) {
			log.error("{} - Failed to store file with originalName: {}", CLASS_NAME, originalName, exception);
			throw exception;
		}
	}

	public Resource load(String storedName) throws IOException {

		Path path = storageDirectory.resolve(storedName).normalize();
		Resource resource = new UrlResource(path.toUri());

		if (!resource.exists() || !resource.isReadable()) {
			log.warn("{} - Stored file not found or unreadable for storedName: {}", CLASS_NAME, storedName);
			throw new IOException("Stored file was not found");
		}

		log.info("{} - File loaded successfully for storedName: {}", CLASS_NAME, storedName);
		return resource;
	}

	public void delete(String storedName) {

		if (storedName == null || storedName.isBlank()) {
			return;
		}

		try {
			boolean deleted = Files.deleteIfExists(storageDirectory.resolve(storedName).normalize());

			if (deleted) {
				log.info("{} - File deleted successfully for storedName: {}", CLASS_NAME, storedName);
			}

		} catch (IOException exception) {
			log.error("{} - Failed to delete file for storedName: {}", CLASS_NAME, storedName, exception);
		}
	}

	public record StoredFile(String originalName, String storedName, String contentType) {
	}
}