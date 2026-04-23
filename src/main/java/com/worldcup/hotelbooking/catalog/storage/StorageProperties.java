package com.worldcup.hotelbooking.catalog.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
     * Local storage settings
     */
    private String uploadDir;

    /**
     * Shared settings
     */
    private String publicBaseUrl;
    private long maxFileSizeBytes;

    /**
     * S3 settings
     */
    private String bucketName;
    private String region;
}