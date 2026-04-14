package com.worldcup.hotelbooking.catalog.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.junit.jupiter.api.Assertions.*;

class StaticResourceConfigTest {

    private StorageProperties storageProperties;
    private GenericWebApplicationContext applicationContext;
    private MockServletContext servletContext;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        servletContext = new MockServletContext();
        applicationContext = new GenericWebApplicationContext(servletContext);
        applicationContext.refresh();
    }

    @Test
    void addResourceHandlers_WhenUploadDirIsNull_ShouldThrowException() {
        storageProperties.setUploadDir(null);
        StaticResourceConfig config = new StaticResourceConfig(storageProperties);

        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(applicationContext, servletContext);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> config.addResourceHandlers(registry)
        );

        assertEquals("app.storage.upload-dir is not configured", ex.getMessage());
    }

    @Test
    void addResourceHandlers_WhenUploadDirIsBlank_ShouldThrowException() {
        storageProperties.setUploadDir("   ");
        StaticResourceConfig config = new StaticResourceConfig(storageProperties);

        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(applicationContext, servletContext);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> config.addResourceHandlers(registry)
        );

        assertEquals("app.storage.upload-dir is not configured", ex.getMessage());
    }

    @Test
    void addResourceHandlers_WhenUploadDirIsValid_ShouldNotThrowException() {
        storageProperties.setUploadDir("/tmp/uploads");
        StaticResourceConfig config = new StaticResourceConfig(storageProperties);

        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(applicationContext, servletContext);

        assertDoesNotThrow(() -> config.addResourceHandlers(registry));
    }
}
