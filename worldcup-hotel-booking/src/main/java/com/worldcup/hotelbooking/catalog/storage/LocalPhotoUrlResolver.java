package com.worldcup.hotelbooking.catalog.storage;

import org.springframework.stereotype.Component;

@Component
public class LocalPhotoUrlResolver implements PhotoUrlResolver {

    private final StorageProperties storageProperties;

    public LocalPhotoUrlResolver(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public String resolve(String storageKey) {
        String baseUrl = storageProperties.getPublicBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl + storageKey;
        }
        return baseUrl + "/" + storageKey;
    }
}