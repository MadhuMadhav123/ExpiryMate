package com.expirymate.document.service;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3StorageService {

    private static final Logger log =
            LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(
            MultipartFile file,
            Long userId,
            Long documentId) {

        try {

            String originalFileName = file.getOriginalFilename();

            String safeFileName =
                    originalFileName == null
                            ? "document"
                            : originalFileName.replaceAll(
                            "[^a-zA-Z0-9._-]",
                            "_");

            String s3Key =
                    "users/"
                            + userId
                            + "/documents/"
                            + documentId
                            + "/"
                            + UUID.randomUUID()
                            + "-"
                            + safeFileName;

            PutObjectRequest request =
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .contentType(file.getContentType())
                            .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(file.getBytes()));

            log.info(
                    "Document file uploaded successfully to S3 for documentId={}",
                    documentId);

            return s3Key;

        } catch (IOException ex) {

            log.error(
                    "Failed to upload document file to S3 for documentId={}",
                    documentId,
                    ex);

            throw new RuntimeException(
                    "Failed to upload document file",
                    ex);
        }
    }

    public byte[] downloadFile(String s3Key) {

        GetObjectRequest request =
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build();

        ResponseBytes<GetObjectResponse> object =
                s3Client.getObjectAsBytes(request);

        return object.asByteArray();
    }

    public void deleteFile(String s3Key) {

        DeleteObjectRequest request =
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build();

        s3Client.deleteObject(request);

        log.info(
                "Document file deleted successfully from S3 for key={}",
                s3Key);
    }
}