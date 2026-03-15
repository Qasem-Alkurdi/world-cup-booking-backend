package com.worldcup.hotelbooking.catalog.storage;

import com.worldcup.hotelbooking.catalog.storage.exception.InvalidPhotoFileException;
import com.worldcup.hotelbooking.catalog.storage.exception.StorageOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalPhotoStorageServiceTest {

    @TempDir
    Path tempDir;

    private StorageProperties storageProperties;
    private LocalPhotoStorageService storageService;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setUploadDir(tempDir.toString());
        storageProperties.setMaxFileSizeBytes(1024 * 1024); // 1 MB

        storageService = new LocalPhotoStorageService(storageProperties);
    }

    @Test
    void store_WhenFileIsNull_ShouldThrowInvalidPhotoFileException() {
        InvalidPhotoFileException ex = assertThrows(
                InvalidPhotoFileException.class,
                () -> storageService.store(null, "hotels")
        );

        assertEquals("Photo file is empty", ex.getMessage());
    }

    @Test
    void store_WhenFileIsEmpty_ShouldThrowInvalidPhotoFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[0]
        );

        InvalidPhotoFileException ex = assertThrows(
                InvalidPhotoFileException.class,
                () -> storageService.store(file, "hotels")
        );

        assertEquals("Photo file is empty", ex.getMessage());
    }

    @Test
    void store_WhenFileExceedsMaxSize_ShouldThrowInvalidPhotoFileException() {
        storageProperties.setMaxFileSizeBytes(3);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4}
        );

        InvalidPhotoFileException ex = assertThrows(
                InvalidPhotoFileException.class,
                () -> storageService.store(file, "hotels")
        );

        assertEquals("Photo file exceeds max allowed size", ex.getMessage());
    }

    @Test
    void store_WhenContentTypeIsUnsupported_ShouldThrowInvalidPhotoFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.gif",
                "image/gif",
                new byte[]{1, 2, 3}
        );

        InvalidPhotoFileException ex = assertThrows(
                InvalidPhotoFileException.class,
                () -> storageService.store(file, "hotels")
        );

        assertEquals("Unsupported photo content type", ex.getMessage());
    }

    @Test
    void store_WhenContentTypeIsNull_ShouldThrowInvalidPhotoFileException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                null,
                new byte[]{1, 2, 3}
        );

        InvalidPhotoFileException ex = assertThrows(
                InvalidPhotoFileException.class,
                () -> storageService.store(file, "hotels")
        );

        assertEquals("Unsupported photo content type", ex.getMessage());
    }

    @Test
    void store_WhenValidJpegFile_ShouldStoreAndReturnStorageKey() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3, 4}
        );

        String storageKey = storageService.store(file, "hotels");

        assertNotNull(storageKey);
        assertTrue(storageKey.startsWith("hotels/"));
        assertTrue(storageKey.endsWith(".jpg"));

        Path storedFile = tempDir.resolve(storageKey);
        assertTrue(Files.exists(storedFile));
        assertArrayEquals(new byte[]{1, 2, 3, 4}, Files.readAllBytes(storedFile));
    }

    @Test
    void store_WhenValidPngFileWithoutExtension_ShouldStoreSuccessfully() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo",
                "image/png",
                new byte[]{9, 8, 7}
        );

        String storageKey = storageService.store(file, "rooms");

        assertNotNull(storageKey);
        assertTrue(storageKey.startsWith("rooms/"));

        String filename = storageKey.substring("rooms/".length());
        assertFalse(filename.isBlank());

        Path storedFile = tempDir.resolve(storageKey);
        assertTrue(Files.exists(storedFile));
        assertArrayEquals(new byte[]{9, 8, 7}, Files.readAllBytes(storedFile));
    }

    @Test
    void store_WhenFilenameEndsWithDot_ShouldStoreWithoutExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.",
                "image/webp",
                new byte[]{5, 6}
        );

        String storageKey = storageService.store(file, "gallery");

        assertTrue(storageKey.startsWith("gallery/"));
        assertFalse(storageKey.endsWith("."));
    }

    @Test
    void store_WhenFolderEscapesRoot_ShouldThrowStorageOperationException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2}
        );

        StorageOperationException ex = assertThrows(
                StorageOperationException.class,
                () -> storageService.store(file, "../../outside")
        );

        assertEquals("Invalid storage target path", ex.getMessage());
    }

    @Test
    void delete_WhenFileExists_ShouldDeleteIt() throws IOException {
        Path folder = tempDir.resolve("hotels");
        Files.createDirectories(folder);
        Path file = folder.resolve("photo.jpg");
        Files.write(file, new byte[]{1, 2, 3});

        assertTrue(Files.exists(file));

        storageService.delete("hotels/photo.jpg");

        assertFalse(Files.exists(file));
    }

    @Test
    void delete_WhenFileDoesNotExist_ShouldNotThrowException() {
        assertDoesNotThrow(() -> storageService.delete("hotels/missing.jpg"));
    }

    @Test
    void delete_WhenStorageKeyEscapesRoot_ShouldThrowStorageOperationException() {
        StorageOperationException ex = assertThrows(
                StorageOperationException.class,
                () -> storageService.delete("../../etc/passwd")
        );

        assertEquals("Invalid storage key", ex.getMessage());
    }

    @Test
    void store_WhenIOExceptionOccurs_ShouldThrowStorageOperationException() {
        StorageProperties badProperties = new StorageProperties();
        badProperties.setUploadDir("/proc/invalid-path-that-cannot-be-created");
        badProperties.setMaxFileSizeBytes(1024 * 1024);

        LocalPhotoStorageService badService = new LocalPhotoStorageService(badProperties);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        StorageOperationException ex = assertThrows(
                StorageOperationException.class,
                () -> badService.store(file, "hotels")
        );

        assertEquals("Failed to store photo file", ex.getMessage());
        assertNotNull(ex.getCause());
    }
}
