package com.worldcup.hotelbooking.catalog.storage;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    /**
     * local | s3
     */
    private String type;

    /**
     * Local
     */
    private String uploadDir;

    /**
     * Shared
     */
    private String publicBaseUrl;
    private long maxFileSizeBytes;

    /**
     * S3
     */
    private String bucketName;
    private String region;

    @PostConstruct
    public void validate() {

        // default fallback (لو ما انحطت في ENV أو YAML)
        if (!StringUtils.hasText(type)) {
            type = "local";
        }

        if ("local".equalsIgnoreCase(type)) {
            validateLocal();
        } else if ("s3".equalsIgnoreCase(type)) {
            validateS3();
        } else {
            throw new IllegalStateException("Invalid storage type: " + type + " (must be 'local' or 's3')");
        }
    }

    private void validateLocal() {
        if (!StringUtils.hasText(uploadDir)) {
            throw new IllegalStateException("app.storage.upload-dir must be configured for local storage");
        }

        if (!StringUtils.hasText(publicBaseUrl)) {
            throw new IllegalStateException("app.storage.public-base-url must be configured for local storage");
        }
    }

    private void validateS3() {
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalStateException("app.storage.bucket-name must be configured for S3");
        }

        if (!StringUtils.hasText(region)) {
            throw new IllegalStateException("app.storage.region must be configured for S3");
        }

        if (!StringUtils.hasText(publicBaseUrl)) {
            throw new IllegalStateException("app.storage.public-base-url must be configured for S3");
        }
    }
}