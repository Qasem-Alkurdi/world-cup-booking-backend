package com.worldcup.hotelbooking.catalog.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties
        (prefix = "app.storage")
public class StorageProperties {

    private String uploadDir;
    private String publicBaseUrl;
    private long maxFileSizeBytes;

}