package com.worldcup.hotelbooking.catalog.storage;

import com.worldcup.hotelbooking.catalog.storage.exception.InvalidPhotoFileException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InvalidPhotoFileExceptionTest {

    @Test
    void constructor_ShouldSetMessage() {
        InvalidPhotoFileException ex = new InvalidPhotoFileException("Invalid photo");

        assertEquals("Invalid photo", ex.getMessage());
        assertNull(ex.getCause());
    }
}
