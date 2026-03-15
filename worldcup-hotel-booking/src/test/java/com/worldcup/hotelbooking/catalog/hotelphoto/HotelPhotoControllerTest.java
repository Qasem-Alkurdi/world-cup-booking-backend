package com.worldcup.hotelbooking.catalog.hotelphoto;

import com.worldcup.hotelbooking.catalog.hotelphoto.dto.HotelPhotoResponseDto;
import com.worldcup.hotelbooking.catalog.hotelphoto.mapper.HotelPhotoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HotelPhotoControllerTest {

    private MockMvc mockMvc;
    private HotelPhotoService hotelPhotoService;
    private HotelPhotoMapper hotelPhotoMapper;

    @BeforeEach
    void setUp() {
        hotelPhotoService = mock(HotelPhotoService.class);
        hotelPhotoMapper = mock(HotelPhotoMapper.class);

        HotelPhotoController controller = new HotelPhotoController(hotelPhotoService, hotelPhotoMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
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
    void all_ShouldReturnPhotos() throws Exception {
        HotelPhoto p1 = buildPhoto(1L, "a.jpg", "caption1", 1, true);
        HotelPhoto p2 = buildPhoto(2L, "b.jpg", "caption2", 2, false);

        HotelPhotoResponseDto r1 = buildResponse(1L, "http://localhost/uploads/a.jpg", "caption1", 1);
        HotelPhotoResponseDto r2 = buildResponse(2L, "http://localhost/uploads/b.jpg", "caption2", 2);

        given(hotelPhotoService.listPhotos(100L)).willReturn(List.of(p1, p2));
        given(hotelPhotoMapper.toResponse(p1)).willReturn(r1);
        given(hotelPhotoMapper.toResponse(p2)).willReturn(r2);

        mockMvc.perform(get("/hotels/{hotelId}/photos", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].url").value("http://localhost/uploads/a.jpg"))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(hotelPhotoService, times(1)).listPhotos(100L);
    }

    @Test
    @DisplayName("POST /hotels/{hotelId}/photos -> should upload photo and return 201")
    void upload_ShouldReturnCreatedPhoto() throws Exception {
        HotelPhoto saved = buildPhoto(1L, "hotels/100/a.jpg", "caption1", 1, true);
        HotelPhotoResponseDto response = buildResponse(
                1L,
                "http://localhost/uploads/hotels/100/a.jpg",
                "caption1",
                1
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        given(hotelPhotoService.addPhoto(eq(100L), any(), eq("caption1"), eq(1)))
                .willReturn(saved);
        given(hotelPhotoMapper.toResponse(saved)).willReturn(response);

        mockMvc.perform(
                        multipart("/hotels/{hotelId}/photos", 100L)
                                .file(file)
                                .param("caption", "caption1")
                                .param("sortOrder", "1")
                )
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/hotels/100/photos/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.caption").value("caption1"))
                .andExpect(jsonPath("$.sortOrder").value(1));

        verify(hotelPhotoService, times(1)).addPhoto(eq(100L), any(), eq("caption1"), eq(1));
    }

    @Test
    @DisplayName("POST /hotels/{hotelId}/photos -> should upload without optional params")
    void upload_WithoutOptionalParams_ShouldReturnCreatedPhoto() throws Exception {
        HotelPhoto saved = buildPhoto(1L, "hotels/100/a.jpg", null, 1, true);
        HotelPhotoResponseDto response = buildResponse(
                1L,
                "http://localhost/uploads/hotels/100/a.jpg",
                null,
                1
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        given(hotelPhotoService.addPhoto(eq(100L), any(), eq(null), eq(null)))
                .willReturn(saved);
        given(hotelPhotoMapper.toResponse(saved)).willReturn(response);

        mockMvc.perform(
                        multipart("/hotels/{hotelId}/photos", 100L)
                                .file(file)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(hotelPhotoService, times(1)).addPhoto(eq(100L), any(), eq(null), eq(null));
    }

    @Test
    @DisplayName("DELETE /hotels/{hotelId}/photos/{photoId} -> should return 204")
    void delete_ShouldReturnNoContent() throws Exception {
        willDoNothing().given(hotelPhotoService).deletePhoto(100L, 1L);

        mockMvc.perform(delete("/hotels/{hotelId}/photos/{photoId}", 100L, 1L))
                .andExpect(status().isNoContent());

        verify(hotelPhotoService, times(1)).deletePhoto(100L, 1L);
    }

    @Test
    @DisplayName("PATCH /hotels/{hotelId}/photos/{photoId}/primary -> should return 204")
    void setPrimary_ShouldReturnNoContent() throws Exception {
        willDoNothing().given(hotelPhotoService).setPrimaryPhoto(100L, 1L);

        mockMvc.perform(patch("/hotels/{hotelId}/photos/{photoId}/primary", 100L, 1L))
                .andExpect(status().isNoContent());

        verify(hotelPhotoService, times(1)).setPrimaryPhoto(100L, 1L);
    }

    @Test
    @DisplayName("PATCH /hotels/{hotelId}/photos/reorder -> should return 204")
    void reorder_ShouldReturnNoContent() throws Exception {
        willDoNothing().given(hotelPhotoService).reorderPhotos(100L, List.of(3L, 1L, 2L));

        String requestBody = """
                {
                  "photoIds": [3, 1, 2]
                }
                """;

        mockMvc.perform(
                        patch("/hotels/{hotelId}/photos/reorder", 100L)
                                .contentType(APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent());

        verify(hotelPhotoService, times(1)).reorderPhotos(100L, List.of(3L, 1L, 2L));
    }
}
