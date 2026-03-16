package com.worldcup.hotelbooking.catalog.roomtypephoto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.hotelbooking.catalog.roomtypephoto.dto.RoomTypePhotoResponseDto;
import com.worldcup.hotelbooking.catalog.roomtypephoto.mapper.RoomTypePhotoMapper;
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

class RoomTypePhotoControllerTest {

    private MockMvc mockMvc;
    private RoomTypePhotoService service;
    private RoomTypePhotoMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        service = mock(RoomTypePhotoService.class);
        mapper = mock(RoomTypePhotoMapper.class);

        RoomTypePhotoController controller = new RoomTypePhotoController(service, mapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        objectMapper = new ObjectMapper();
    }

    private RoomTypePhoto photo(Long id, String storageKey, String caption, Integer sortOrder, boolean primary) {
        RoomTypePhoto p = new RoomTypePhoto();
        p.setId(id);
        p.setStorageKey(storageKey);
        p.setCaption(caption);
        p.setSortOrder(sortOrder);
        p.setPrimary(primary);
        p.setCreatedAt(OffsetDateTime.parse("2026-03-15T10:00:00Z"));
        return p;
    }

    private RoomTypePhotoResponseDto dto(Long id, String url, String caption, Integer sortOrder) {
        return new RoomTypePhotoResponseDto(
                id,
                url,
                caption,
                sortOrder,
                OffsetDateTime.parse("2026-03-15T10:00:00Z")
        );
    }

    @Test
    @DisplayName("GET /hotels/{hotelId}/room-types/{roomTypeId}/photos -> should return photos")
    void listPhotos_ShouldReturnPhotos() throws Exception {
        RoomTypePhoto p1 = photo(1L, "a.jpg", "caption1", 1, true);
        RoomTypePhoto p2 = photo(2L, "b.jpg", "caption2", 2, false);

        RoomTypePhotoResponseDto d1 = dto(1L, "http://localhost/uploads/a.jpg", "caption1", 1);
        RoomTypePhotoResponseDto d2 = dto(2L, "http://localhost/uploads/b.jpg", "caption2", 2);

        given(service.listPhotos(100L, 10L)).willReturn(List.of(p1, p2));
        given(mapper.toResponse(p1)).willReturn(d1);
        given(mapper.toResponse(p2)).willReturn(d2);

        mockMvc.perform(get("/hotels/{hotelId}/room-types/{roomTypeId}/photos", 100L, 10L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].url").value("http://localhost/uploads/a.jpg"))
                .andExpect(jsonPath("$[0].caption").value("caption1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].url").value("http://localhost/uploads/b.jpg"));

        then(service).should().listPhotos(100L, 10L);
    }

    @Test
    @DisplayName("GET /hotels/{hotelId}/room-types/{roomTypeId}/photos -> should return empty list")
    void listPhotos_WhenEmpty_ShouldReturnEmptyList() throws Exception {
        given(service.listPhotos(100L, 10L)).willReturn(List.of());

        mockMvc.perform(get("/hotels/{hotelId}/room-types/{roomTypeId}/photos", 100L, 10L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        then(service).should().listPhotos(100L, 10L);
    }

    @Test
    @DisplayName("POST /hotels/{hotelId}/room-types/{roomTypeId}/photos -> should upload and return 201")
    void uploadPhoto_ShouldReturnCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        RoomTypePhoto saved = photo(
                1L,
                "hotels/100/room-types/10/a.jpg",
                "caption1",
                1,
                true
        );

        RoomTypePhotoResponseDto response = dto(
                1L,
                "http://localhost/uploads/hotels/100/room-types/10/a.jpg",
                "caption1",
                1
        );

        given(service.addPhoto(
                eq(100L),
                eq(10L),
                any(org.springframework.web.multipart.MultipartFile.class),
                any(),
                any()
        )).willReturn(saved);

        given(mapper.toResponse(saved)).willReturn(response);

        mockMvc.perform(
                        multipart("/hotels/{hotelId}/room-types/{roomTypeId}/photos", 100L, 10L)
                                .file(file)
                                .param("caption", "caption1")
                                .param("sortOrder", "1")
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/hotels/100/room-types/10/photos/1"
                ))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.url").value("http://localhost/uploads/hotels/100/room-types/10/a.jpg"))
                .andExpect(jsonPath("$.caption").value("caption1"))
                .andExpect(jsonPath("$.sortOrder").value(1));

        then(service).should().addPhoto(
                eq(100L),
                eq(10L),
                any(org.springframework.web.multipart.MultipartFile.class),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("POST /hotels/{hotelId}/room-types/{roomTypeId}/photos -> should upload without optional params")
    void uploadPhoto_WithoutOptionalParams_ShouldReturnCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        RoomTypePhoto saved = photo(
                1L,
                "hotels/100/room-types/10/a.jpg",
                null,
                5,
                true
        );

        RoomTypePhotoResponseDto response = dto(
                1L,
                "http://localhost/uploads/hotels/100/room-types/10/a.jpg",
                null,
                5
        );

        given(service.addPhoto(
                eq(100L),
                eq(10L),
                any(org.springframework.web.multipart.MultipartFile.class),
                isNull(),
                isNull()
        )).willReturn(saved);

        given(mapper.toResponse(saved)).willReturn(response);

        mockMvc.perform(
                        multipart("/hotels/{hotelId}/room-types/{roomTypeId}/photos", 100L, 10L)
                                .file(file)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.sortOrder").value(5));

        then(service).should().addPhoto(
                eq(100L),
                eq(10L),
                any(org.springframework.web.multipart.MultipartFile.class),
                isNull(),
                isNull()
        );
    }


    @Test
    @DisplayName("DELETE /hotels/{hotelId}/room-types/{roomTypeId}/photos/{photoId} -> should return 204")
    void deletePhoto_ShouldReturnNoContent() throws Exception {
        willDoNothing().given(service).deletePhoto(100L, 10L, 1L);

        mockMvc.perform(delete(
                        "/hotels/{hotelId}/room-types/{roomTypeId}/photos/{photoId}",
                        100L, 10L, 1L
                ))
                .andExpect(status().isNoContent());

        then(service).should().deletePhoto(100L, 10L, 1L);
    }

    @Test
    @DisplayName("PATCH /hotels/{hotelId}/room-types/{roomTypeId}/photos/{photoId}/primary -> should return 204")
    void setPrimary_ShouldReturnNoContent() throws Exception {
        willDoNothing().given(service).setPrimaryPhoto(100L, 10L, 1L);

        mockMvc.perform(patch(
                        "/hotels/{hotelId}/room-types/{roomTypeId}/photos/{photoId}/primary",
                        100L, 10L, 1L
                ))
                .andExpect(status().isNoContent());

        then(service).should().setPrimaryPhoto(100L, 10L, 1L);
    }

    @Test
    @DisplayName("PATCH /hotels/{hotelId}/room-types/{roomTypeId}/photos/reorder -> should return 204")
    void reorderPhotos_ShouldReturnNoContent() throws Exception {
        willDoNothing().given(service).reorderPhotos(100L, 10L, List.of(3L, 1L, 2L));

        String requestBody = objectMapper.writeValueAsString(
                new com.worldcup.hotelbooking.catalog.roomtypephoto.dto.ReorderPhotosRequestDto() {{
                    setPhotoIds(List.of(3L, 1L, 2L));
                }}
        );

        mockMvc.perform(
                        patch("/hotels/{hotelId}/room-types/{roomTypeId}/photos/reorder", 100L, 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent());

        then(service).should().reorderPhotos(100L, 10L, List.of(3L, 1L, 2L));
    }
}