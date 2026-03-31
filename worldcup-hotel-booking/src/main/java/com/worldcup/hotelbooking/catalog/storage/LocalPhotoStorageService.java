package com.worldcup.hotelbooking.catalog.storage;

import com.worldcup.hotelbooking.catalog.storage.exception.InvalidPhotoFileException;
import com.worldcup.hotelbooking.catalog.storage.exception.StorageOperationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalPhotoStorageService implements PhotoStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
            , "image/jpg"
    );

    private final StorageProperties storageProperties;

    public LocalPhotoStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public String store(MultipartFile file, String folder) {
        validate(file);

        String extension = extractExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);

        Path rootPath = Paths.get(storageProperties.getUploadDir()).normalize().toAbsolutePath();
        Path folderPath = rootPath.resolve(folder).normalize();
        Path targetPath = folderPath.resolve(filename).normalize();

        if (!targetPath.startsWith(rootPath)) {
            throw new StorageOperationException("Invalid storage target path");
        }

        try {
            Files.createDirectories(folderPath);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageOperationException("Failed to store photo file", e);
        }

        return folder + "/" + filename;
    }

    @Override
    public void delete(String storageKey) {
        Path rootPath = Paths.get(storageProperties.getUploadDir()).normalize().toAbsolutePath();
        Path filePath = rootPath.resolve(storageKey).normalize();

        if (!filePath.startsWith(rootPath)) {
            throw new StorageOperationException("Invalid storage key");
        }

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new StorageOperationException("Failed to delete photo file", e);
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
}