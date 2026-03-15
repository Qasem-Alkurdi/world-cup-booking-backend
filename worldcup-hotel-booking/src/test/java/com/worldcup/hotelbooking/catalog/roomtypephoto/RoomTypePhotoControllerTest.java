package com.worldcup.hotelbooking.catalog.roomtypephoto;

import com.worldcup.hotelbooking.catalog.roomtypephoto.dto.RoomTypePhotoResponseDto;
import com.worldcup.hotelbooking.catalog.roomtypephoto.mapper.RoomTypePhotoMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RoomTypePhotoControllerTest {

    private MockMvc mockMvc;
    private RoomTypePhotoService roomTypePhotoService;
    private RoomTypePhotoMapper roomTypePhotoMapper;

    @BeforeEach
    void setUp() {
        roomTypePhotoService = mock(RoomTypePhotoService.class);
        roomTypePhotoMapper = mock(RoomTypePhotoMapper.class);

        RoomTypePhotoController controller =
                new RoomTypePhotoController(roomTypePhotoService, roomTypePhotoMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private RoomTypePhoto buildPhoto(Long id, String storageKey, String caption, Integer sortOrder, boolean primary) {
        RoomTypePhoto photo = new RoomTypePhoto();
        photo.setId(id);
        photo.setStorageKey(storageKey);
        photo.setCaption(caption);
        photo.setSortOrder(sortOrder);
        photo.setPrimary(primary);
        photo.setCreatedAt(OffsetDateTime.parse("2026-03-15T10:00:00Z"));
        return photo;
    }

    private RoomTypePhotoResponseDto buildResponse(Long id, String url, String caption, Integer sortOrder) {
        return new RoomTypePhotoResponseDto(
                id,
                url,
                caption,
                sortOrder,
                OffsetDateTime.parse("2026-03-15T10:00:00Z")
        );
    }

    @Test
    @DisplayName("GET /photos -> should return all photos")
    void all_ShouldReturnPhotos() throws Exception {
        RoomTypePhoto p1 = buildPhoto(1L, "a.jpg", "caption1", 1, true);
        RoomTypePhoto p2 = buildPhoto(2L, "b.jpg", "caption2", 2, false);

        RoomTypePhotoResponseDto r1 = buildResponse(1L, "http://localhost/uploads/a.jpg", "caption1", 1);
        RoomTypePhotoResponseDto r2 = buildResponse(2L, "http://localhost/uploads/b.jpg", "caption2", 2);

        given(roomTypePhotoService.listPhotos(100L, 10L)).willReturn(List.of(p1, p2));
        given(roomTypePhotoMapper.toResponse(p1)).willReturn(r1);
        given(roomTypePhotoMapper.toResponse(p2)).willReturn(r2);

        mockMvc.perform(get("/hotels/{hotelId}/room-types/{roomTypeId}/photos", 100L, 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].url").value("http://localhost/uploads/a.jpg"))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(roomTypePhotoService, times(1)).listPhotos(100L, 10L);
    }

    @Test
    @DisplayName("POST /photos -> should upload photo and return 201")
    void upload_ShouldReturnCreatedPhoto() throws Exception {
        RoomTypePhoto saved = buildPhoto(1L, "hotels/100/room-types/10/a.jpg", "caption1", 1, true);
        RoomTypePhotoResponseDto response = buildResponse(
                1L,
                "http://localhost/uploads/hotels/100/room-types/10/a.jpg",
                "caption1",
                1
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        given(roomTypePhotoService.addPhoto(eq(100L), eq(10L), any(), eq("caption1"), eq(1)))
                .willReturn(saved);
        given(roomTypePhotoMapper.toResponse(saved)).willReturn(response);

        mockMvc.perform(
                        multipart("/hotels/{hotelId}/room-types/{roomTypeId}/photos", 100L, 10L)
                                .file(file)
                                .param("caption", "caption1")
                                .param("sortOrder", "1")
                )
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "http://localhost/hotels/100/room-types/10/photos/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.caption").value("caption1"))
                .andExpect(jsonPath("$.sortOrder").value(1));

        verify(roomTypePhotoService, times(1))
                .addPhoto(eq(100L), eq(10L), any(), eq("caption1"), eq(1));
    }

    @Test
    @DisplayName("POST /photos -> should upload photo without optional params")
    void upload_WithoutOptionalParams_ShouldReturnCreatedPhoto() throws Exception {
        RoomTypePhoto saved = buildPhoto(1L, "hotels/100/room-types/10/a.jpg", null, 5, true);
        RoomTypePhotoResponseDto response = buildResponse(
                1L,
                "http://localhost/uploads/hotels/100/room-types/10/a.jpg",
                null,
                5
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        given(roomTypePhotoService.addPhoto(eq(100L), eq(10L), any(), eq(null), eq(null)))
                .willReturn(saved);
        given(roomTypePhotoMapper.toResponse(saved)).willReturn(response);

        mockMvc.perform(
                        multipart("/hotels/{hotelId}/room-types/{roomTypeId}/photos", 100L, 10L)
                                .file(file)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(roomTypePhotoService, times(1))
                .addPhoto(eq(100L), eq(10L), any(), eq(null), eq(null));
    }

    @Test
    @DisplayName("DELETE /photos/{photoId} -> should return 204")
    void delete_ShouldReturnNoContent() throws Exception {
        willDoNothing().given(roomTypePhotoService).deletePhoto(100L, 10L, 1L);

        mockMvc.perform(delete("/hotels/{hotelId}/room-types/{roomTypeId}/photos/{photoId}", 100L, 10L, 1L))
                .andExpect(status().isNoContent());

        verify(roomTypePhotoService, times(1)).deletePhoto(100L, 10L, 1L);
    }

    @Test
    @DisplayName("PATCH /photos/{photoId}/primary -> should return 204")
    void setPrimary_ShouldReturnNoContent() throws Exception {
        willDoNothing().given(roomTypePhotoService).setPrimaryPhoto(100L, 10L, 1L);

        mockMvc.perform(patch("/hotels/{hotelId}/room-types/{roomTypeId}/photos/{photoId}/primary", 100L, 10L, 1L))
                .andExpect(status().isNoContent());

        verify(roomTypePhotoService, times(1)).setPrimaryPhoto(100L, 10L, 1L);
    }

    @Test
    @DisplayName("PATCH /photos/reorder -> should return 204")
    void reorder_ShouldReturnNoContent() throws Exception {
        willDoNothing().given(roomTypePhotoService).reorderPhotos(100L, 10L, List.of(3L, 1L, 2L));

        String requestBody = """
                {
                  "photoIds": [3, 1, 2]
                }
                """;

        mockMvc.perform(
                        patch("/hotels/{hotelId}/room-types/{roomTypeId}/photos/reorder", 100L, 10L)
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNoContent());

        verify(roomTypePhotoService, times(1))
                .reorderPhotos(100L, 10L, List.of(3L, 1L, 2L));
    }
}
