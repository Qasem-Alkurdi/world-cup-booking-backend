package com.worldcup.hotelbooking.catalog.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3PhotoUrlResolver implements PhotoUrlResolver {

    private final StorageProperties storageProperties;

    public S3PhotoUrlResolver(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public String resolve(String storageKey) {
        String baseUrl = storageProperties.getPublicBaseUrl();

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.storage.public-base-url is not configured");
        }

        if (baseUrl.endsWith("/")) {
            return baseUrl + storageKey;
        }

        return baseUrl + "/" + storageKey;
    }
}