package com.worldcup.hotelbooking.catalog.storage;

import com.worldcup.hotelbooking.catalog.storage.exception.InvalidPhotoFileException;
import com.worldcup.hotelbooking.catalog.storage.exception.StorageOperationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3PhotoStorageService implements PhotoStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/jpg"
    );

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public S3PhotoStorageService(S3Client s3Client, StorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
    }

    @Override
    public String store(MultipartFile file, String folder) {
        validate(file);

        String extension = extractExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
        String key = normalizeFolder(folder) + "/" + filename;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(storageProperties.getBucketName())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return key;
        } catch (IOException e) {
            throw new StorageOperationException("Failed to read photo file", e);
        } catch (S3Exception e) {
            throw new StorageOperationException("Failed to upload photo file to S3", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        if (!StringUtils.hasText(storageKey)) {
            throw new StorageOperationException("Storage key must not be blank");
        }

        String cleanKey = StringUtils.cleanPath(storageKey);
        if (cleanKey.contains("..")) {
            throw new StorageOperationException("Invalid storage key");
        }

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(storageProperties.getBucketName())
                    .key(cleanKey)
                    .build();

            s3Client.deleteObject(request);
        } catch (S3Exception e) {
            throw new StorageOperationException("Failed to delete photo file from S3", e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidPhotoFileException("Photo file is empty");
        }

        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new InvalidPhotoFileException("Photo file exceeds max allowed size");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidPhotoFileException("Unsupported photo content type");
        }
    }

    private String extractExtension(String originalFilename) {
        String cleanName = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int lastDot = cleanName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == cleanName.length() - 1) {
            return "";
        }
        return cleanName.substring(lastDot + 1).toLowerCase();
    }

    private String normalizeFolder(String folder) {
        String cleanFolder = StringUtils.cleanPath(folder == null ? "" : folder).replace("\\", "/");

        while (cleanFolder.startsWith("/")) {
            cleanFolder = cleanFolder.substring(1);
        }

        while (cleanFolder.endsWith("/")) {
            cleanFolder = cleanFolder.substring(0, cleanFolder.length() - 1);
        }

        if (cleanFolder.isBlank() || cleanFolder.contains("..")) {
            throw new StorageOperationException("Invalid folder");
        }

        return cleanFolder;
    }
}