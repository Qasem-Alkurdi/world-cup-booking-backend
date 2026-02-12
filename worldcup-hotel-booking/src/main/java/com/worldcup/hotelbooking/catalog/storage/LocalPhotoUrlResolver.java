package com.worldcup.hotelbooking.catalog.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalPhotoUrlResolver implements PhotoUrlResolver {

    private final String baseUrl;

    public LocalPhotoUrlResolver(
            @Value("${app.storage.public-base-url}") String baseUrl
    ) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String resolve(String storageKey) {
        return baseUrl + "/" + storageKey;
    }
}
