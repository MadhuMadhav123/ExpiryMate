package com.expirymate.document.service;

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

	private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");

	private final Path storageDirectory;

	public FileStorageService(@Value("${app.storage.path:uploads}") String storagePath) throws IOException {
		this.storageDirectory = Paths.get(storagePath).toAbsolutePath().normalize();
		Files.createDirectories(storageDirectory);
	}

	public StoredFile store(MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Please select a document file");
		}
		if (file.getSize() > 10 * 1024 * 1024) {
			throw new IllegalArgumentException("File size must be 10 MB or less");
		}
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
			throw new IllegalArgumentException("Only PDF, JPG and PNG files are allowed");
		}

		String originalName = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
		String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
		String storedName = UUID.randomUUID() + extension;
		Path target = storageDirectory.resolve(storedName).normalize();
		Files.copy(file.getInputStream(), target);
		return new StoredFile(originalName, storedName, contentType);
	}

	public Resource load(String storedName) throws IOException {
		Path path = storageDirectory.resolve(storedName).normalize();
		Resource resource = new UrlResource(path.toUri());
		if (!resource.exists() || !resource.isReadable()) {
			throw new IOException("Stored file was not found");
		}
		return resource;
	}

	public void delete(String storedName) {
		if (storedName == null || storedName.isBlank()) {
			return;
		}
		try {
			Files.deleteIfExists(storageDirectory.resolve(storedName).normalize());
		} catch (IOException ignored) {
		}
	}

	public record StoredFile(String originalName, String storedName, String contentType) {
	}
}
