package com.expirymate.document.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

	private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
	private static final String CLASS_NAME = FileStorageService.class.getSimpleName();

	private static final Set<String> ALLOWED_TYPES =
			Set.of("application/pdf", "image/jpeg", "image/png");

	private final S3Client s3Client;
	private final String bucketName;

	public FileStorageService(
			S3Client s3Client,
			@Value("${aws.s3.bucket-name}") String bucketName) {

		this.s3Client = s3Client;
		this.bucketName = bucketName;
	}

	public StoredFile store(MultipartFile file) throws IOException {

		if (file == null || file.isEmpty()) {
			log.warn("{} - File upload validation failed: no file selected", CLASS_NAME);
			throw new IllegalArgumentException("Please select a document file");
		}

		if (file.getSize() > 10 * 1024 * 1024) {
			log.warn(
					"{} - File upload validation failed for fileName: {}, reason: file size exceeds 10 MB",
					CLASS_NAME,
					file.getOriginalFilename()
			);

			throw new IllegalArgumentException("File size must be 10 MB or less");
		}

		String contentType = file.getContentType();

		if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
			log.warn(
					"{} - File upload validation failed for fileName: {}, contentType: {}",
					CLASS_NAME,
					file.getOriginalFilename(),
					contentType
			);

			throw new IllegalArgumentException(
					"Only PDF, JPG and PNG files are allowed"
			);
		}

		String originalName =
				file.getOriginalFilename() == null
						? "document"
						: file.getOriginalFilename();

		String extension =
				originalName.contains(".")
						? originalName.substring(originalName.lastIndexOf('.'))
						: "";

		String storedName =
				"documents/" + UUID.randomUUID() + extension;

		try {

			PutObjectRequest request =
					PutObjectRequest.builder()
							.bucket(bucketName)
							.key(storedName)
							.contentType(contentType)
							.build();

			s3Client.putObject(
					request,
					RequestBody.fromInputStream(
							file.getInputStream(),
							file.getSize()
					)
			);

			log.info(
					"{} - File stored successfully in S3 with originalName: {}, storedName: {}",
					CLASS_NAME,
					originalName,
					storedName
			);

			return new StoredFile(
					originalName,
					storedName,
					contentType
			);

		} catch (Exception exception) {

			log.error(
					"{} - Failed to store file in S3 with originalName: {}",
					CLASS_NAME,
					originalName,
					exception
			);

			if (exception instanceof IOException ioException) {
				throw ioException;
			}

			throw new IOException(
					"Failed to store document file in S3",
					exception
			);
		}
	}

	public Resource load(String storedName) throws IOException {

		if (storedName == null || storedName.isBlank()) {
			log.warn("{} - File load failed: storedName is empty", CLASS_NAME);
			throw new IOException("Stored file was not found");
		}

		try {

			GetObjectRequest request =
					GetObjectRequest.builder()
							.bucket(bucketName)
							.key(storedName)
							.build();

			ResponseBytes<GetObjectResponse> response =
					s3Client.getObjectAsBytes(request);

			log.info(
					"{} - File loaded successfully from S3 for storedName: {}",
					CLASS_NAME,
					storedName
			);

			return new ByteArrayResource(response.asByteArray()) {
				@Override
				public String getFilename() {
					return storedName;
				}
			};

		} catch (Exception exception) {

			log.error(
					"{} - Failed to load file from S3 for storedName: {}",
					CLASS_NAME,
					storedName,
					exception
			);

			throw new IOException(
					"Stored file was not found",
					exception
			);
		}
	}

	public void delete(String storedName) {

		if (storedName == null || storedName.isBlank()) {
			return;
		}

		try {

			DeleteObjectRequest request =
					DeleteObjectRequest.builder()
							.bucket(bucketName)
							.key(storedName)
							.build();

			s3Client.deleteObject(request);

			log.info(
					"{} - File deleted successfully from S3 for storedName: {}",
					CLASS_NAME,
					storedName
			);

		} catch (Exception exception) {

			log.error(
					"{} - Failed to delete file from S3 for storedName: {}",
					CLASS_NAME,
					storedName,
					exception
			);
		}
	}

	public record StoredFile(
			String originalName,
			String storedName,
			String contentType) {
	}
}