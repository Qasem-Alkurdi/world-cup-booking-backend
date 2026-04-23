package com.worldcup.hotelbooking.catalog.hotelphoto;

import com.worldcup.hotelbooking.catalog.hotelphoto.dto.HotelPhotoResponseDto;
import com.worldcup.hotelbooking.catalog.hotelphoto.mapper.HotelPhotoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HotelPhotoControllerTest {

    private MockMvc mockMvc;
    private HotelPhotoService service;
    private HotelPhotoMapper mapper;

    @BeforeEach
    void setUp() {
        service = mock(HotelPhotoService.class);
        mapper = mock(HotelPhotoMapper.class);

        HotelPhotoController controller =
                new HotelPhotoController(service, mapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    private HotelPhoto buildPhoto(Long id, String storageKey, String caption, Integer sortOrder, boolean primary) {
        HotelPhoto photo = new HotelPhoto();
        photo.setId(id);
        photo.setStorageKey(storageKey);
        photo.setCaption(caption);
        photo.setSortOrder(sortOrder);
        photo.setPrimary(primary);
        photo.setCreatedAt(OffsetDateTime.parse("2026-03-15T10:00:00Z"));
        return photo;
    }

    private HotelPhotoResponseDto buildResponse(Long id, String url, String caption, Integer sortOrder) {
        return new HotelPhotoResponseDto(
                id,
                url,
                caption,
                sortOrder,
                OffsetDateTime.parse("2026-03-15T10:00:00Z")
        );
    }

    @Test
    @DisplayName("GET /hotels/{hotelId}/photos -> should return all photos")
    void listPhotos_ShouldReturnPhotos() throws Exception {

        HotelPhoto p1 = buildPhoto(1L, "a.jpg", "caption1", 1, true);
        HotelPhoto p2 = buildPhoto(2L, "b.jpg", "caption2", 2, false);

        HotelPhotoResponseDto r1 = buildResponse(1L, "http://localhost/uploads/a.jpg", "caption1", 1);
        HotelPhotoResponseDto r2 = buildResponse(2L, "http://localhost/uploads/b.jpg", "caption2", 2);

        given(service.listPhotos(100L)).willReturn(List.of(p1, p2));
        given(mapper.toResponse(p1)).willReturn(r1);
        given(mapper.toResponse(p2)).willReturn(r2);

        mockMvc.perform(get("/hotels/{hotelId}/photos", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].url").value("http://localhost/uploads/a.jpg"))
                .andExpect(jsonPath("$[1].id").value(2));

        then(service).should().listPhotos(100L);
    }

    @Test
    @DisplayName("POST /hotels/{hotelId}/photos -> should upload photo")
    void uploadPhoto_ShouldReturnCreated() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        HotelPhoto saved = buildPhoto(
                1L,
                "hotels/100/a.jpg",
                "caption1",
                1,
                true
        );

        HotelPhotoResponseDto response = buildResponse(
                1L,
                "http://localhost/uploads/hotels/100/a.jpg",
                "caption1",
                1
        );

        given(service.addPhoto(
                eq(100L),
                any(org.springframework.web.multipart.MultipartFile.class),
                any(),
                any()
        )).willReturn(saved);

        given(mapper.toResponse(saved)).willReturn(response);

        mockMvc.perform(
                        multipart("/hotels/{hotelId}/photos", 100L)
                                .file(file)
                                .param("caption", "caption1")
                                .param("sortOrder", "1")
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/hotels/100/photos/1"
                ))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.caption").value("caption1"));

        then(service).should().addPhoto(
                eq(100L),
                any(org.springframework.web.multipart.MultipartFile.class),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("POST /hotels/{hotelId}/photos -> upload without optional params")
    void uploadPhoto_WithoutOptionalParams() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        HotelPhoto saved = buildPhoto(
                1L,
                "hotels/100/a.jpg",
                null,
                5,
                true
        );

        HotelPhotoResponseDto response = buildResponse(
                1L,
                "http://localhost/uploads/hotels/100/a.jpg",
                null,
                5
        );

        given(service.addPhoto(
                eq(100L),
                any(org.springframework.web.multipart.MultipartFile.class),
                isNull(),
                isNull()
        )).willReturn(saved);

        given(mapper.toResponse(saved)).willReturn(response);

        mockMvc.perform(
                        multipart("/hotels/{hotelId}/photos", 100L)
                                .file(file)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        then(service).should().addPhoto(
                eq(100L),
                any(org.springframework.web.multipart.MultipartFile.class),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("DELETE /hotels/{hotelId}/photos/{photoId}")
    void deletePhoto_ShouldReturnNoContent() throws Exception {

        willDoNothing().given(service).deletePhoto(100L, 1L);

        mockMvc.perform(delete("/hotels/{hotelId}/photos/{photoId}", 100L, 1L))
                .andExpect(status().isNoContent());

        then(service).should().deletePhoto(100L, 1L);
    }

    @Test
    @DisplayName("PATCH /hotels/{hotelId}/photos/{photoId}/primary")
    void setPrimary_ShouldReturnNoContent() throws Exception {

        willDoNothing().given(service).setPrimaryPhoto(100L, 1L);

        mockMvc.perform(
                        patch("/hotels/{hotelId}/photos/{photoId}/primary", 100L, 1L)
                )
                .andExpect(status().isNoContent());

        then(service).should().setPrimaryPhoto(100L, 1L);
    }

    @Test
    @DisplayName("PATCH /hotels/{hotelId}/photos/reorder")
    void reorderPhotos_ShouldReturnNoContent() throws Exception {

        willDoNothing().given(service).reorderPhotos(100L, List.of(3L, 1L, 2L));

        String body = """
                {
                  "photoIds":[3,1,2]
                }
                """;

        mockMvc.perform(
                        patch("/hotels/{hotelId}/photos/reorder", 100L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isNoContent());

        then(service).should().reorderPhotos(100L, List.of(3L, 1L, 2L));
    }
}