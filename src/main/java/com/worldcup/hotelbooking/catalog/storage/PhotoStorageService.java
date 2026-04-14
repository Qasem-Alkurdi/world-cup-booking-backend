package com.worldcup.hotelbooking.catalog.storage;

import org.springframework.web.multipart.MultipartFile;

public interface PhotoStorageService {
    String store(MultipartFile file, String folder);

    void delete(String storageKey);
}