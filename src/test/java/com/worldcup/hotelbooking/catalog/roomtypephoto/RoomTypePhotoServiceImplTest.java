package com.worldcup.hotelbooking.catalog.roomtypephoto;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotel.exception.HotelNotFoundException;
import com.worldcup.hotelbooking.catalog.hotelphoto.exception.InvalidPhotoOrderException;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import com.worldcup.hotelbooking.catalog.roomtype.exception.RoomTypeNotFoundException;
import com.worldcup.hotelbooking.catalog.roomtypephoto.exception.RoomTypePhotoNotFoundException;
import com.worldcup.hotelbooking.catalog.storage.PhotoStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.worldcup.hotelbooking.catalog.hotel.HotelStatus.APPROVED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class RoomTypePhotoServiceImplTest {

    private RoomTypePhotoRepository roomTypePhotoRepository;
    private RoomTypeRepository roomTypeRepository;
    private HotelRepository hotelRepository;
    private PhotoStorageService photoStorageService;
    private RoomTypePhotoServiceImpl service;

    @BeforeEach
    void setUp() {
        roomTypePhotoRepository = mock(RoomTypePhotoRepository.class);
        roomTypeRepository = mock(RoomTypeRepository.class);
        hotelRepository = mock(HotelRepository.class);
        photoStorageService = mock(PhotoStorageService.class);

        service = new RoomTypePhotoServiceImpl(
                roomTypePhotoRepository,
                roomTypeRepository,
                hotelRepository,
                photoStorageService
        );
    }

    private Hotel buildHotel(Long id) {
        Hotel hotel = new Hotel();
        hotel.setId(id);
        hotel.setStatus(APPROVED);
        hotel.setDeleted(false);
        return hotel;
    }

    private RoomType buildRoomType(Long id, Long hotelId) {
        Hotel hotel = buildHotel(hotelId);

        RoomType roomType = new RoomType();
        roomType.setId(id);
        roomType.setHotel(hotel);
        roomType.setName("Standard");
        return roomType;
    }

    private RoomTypePhoto buildPhoto(Long id, Long roomTypeId, String key, int sortOrder, boolean primary) {
        RoomType roomType = new RoomType();
        roomType.setId(roomTypeId);

        RoomTypePhoto photo = new RoomTypePhoto();
        photo.setId(id);
        photo.setRoomType(roomType);
        photo.setStorageKey(key);
        photo.setCaption("caption");
        photo.setSortOrder(sortOrder);
        photo.setPrimary(primary);
        photo.setCreatedAt(OffsetDateTime.parse("2026-03-15T10:00:00Z"));
        return photo;
    }

    private void mockValidHotelAndRoomType(Long hotelId, Long roomTypeId) {
        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(hotelId, APPROVED))
                .willReturn(Optional.of(buildHotel(hotelId)));
        given(roomTypeRepository.findByIdAndHotelIdAndHotelNotDeleted(roomTypeId, hotelId))
                .willReturn(Optional.of(buildRoomType(roomTypeId, hotelId)));
    }

    @Test
    @DisplayName("addPhoto -> should append photo at end when sortOrder is null")
    void addPhoto_WhenSortOrderIsNull_ShouldAppendAtEnd() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        RoomTypePhoto existing1 = buildPhoto(1L, roomTypeId, "a.jpg", 1, true);
        RoomTypePhoto existing2 = buildPhoto(2L, roomTypeId, "b.jpg", 2, false);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        RoomTypePhoto savedPhoto = new RoomTypePhoto();
        savedPhoto.setId(3L);
        savedPhoto.setRoomType(buildRoomType(roomTypeId, hotelId));
        savedPhoto.setStorageKey("hotels/100/room-types/10/photo.jpg");
        savedPhoto.setCaption("caption");
        savedPhoto.setSortOrder(3);
        savedPhoto.setPrimary(false);

        given(roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId))
                .willReturn(
                        List.of(existing1, existing2),
                        List.of(existing1, existing2, savedPhoto)
                );

        given(photoStorageService.store(file, "hotels/100/room-types/10"))
                .willReturn("hotels/100/room-types/10/photo.jpg");

        when(roomTypePhotoRepository.save(any(RoomTypePhoto.class)))
                .thenReturn(savedPhoto);

        RoomTypePhoto result = service.addPhoto(hotelId, roomTypeId, file, "caption", null);

        assertEquals("hotels/100/room-types/10/photo.jpg", result.getStorageKey());
        assertEquals("caption", result.getCaption());
        assertEquals(3, result.getSortOrder());
        assertFalse(result.isPrimary());
    }

    @Test
    @DisplayName("addPhoto -> should make first photo primary when no photos exist")
    void addPhoto_WhenNoPhotosExist_ShouldMakePhotoPrimary() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        RoomTypePhoto savedPhoto = new RoomTypePhoto();
        savedPhoto.setId(1L);
        savedPhoto.setRoomType(buildRoomType(roomTypeId, hotelId));
        savedPhoto.setStorageKey("hotels/100/room-types/10/photo.jpg");
        savedPhoto.setCaption("caption");
        savedPhoto.setSortOrder(1);
        savedPhoto.setPrimary(true);

        given(roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId))
                .willReturn(
                        List.of(),
                        List.of(savedPhoto)
                );

        given(photoStorageService.store(file, "hotels/100/room-types/10"))
                .willReturn("hotels/100/room-types/10/photo.jpg");

        when(roomTypePhotoRepository.save(any(RoomTypePhoto.class)))
                .thenReturn(savedPhoto);

        RoomTypePhoto result = service.addPhoto(hotelId, roomTypeId, file, "caption", null);

        assertEquals(1, result.getSortOrder());
        assertTrue(result.isPrimary());
    }

    @Test
    @DisplayName("addPhoto -> should insert photo at requested sortOrder and shift following photos")
    void addPhoto_WhenSortOrderProvided_ShouldShiftExistingPhotos() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        RoomTypePhoto existing1 = buildPhoto(1L, roomTypeId, "a.jpg", 1, true);
        RoomTypePhoto existing2 = buildPhoto(2L, roomTypeId, "b.jpg", 2, false);
        RoomTypePhoto existing3 = buildPhoto(3L, roomTypeId, "c.jpg", 3, false);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        RoomTypePhoto savedPhoto = new RoomTypePhoto();
        savedPhoto.setId(4L);
        savedPhoto.setRoomType(buildRoomType(roomTypeId, hotelId));
        savedPhoto.setStorageKey("hotels/100/room-types/10/photo.jpg");
        savedPhoto.setCaption("caption");
        savedPhoto.setSortOrder(2);
        savedPhoto.setPrimary(false);

        given(roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId))
                .willReturn(
                        List.of(existing1, existing2, existing3),
                        List.of(existing1, savedPhoto, existing2, existing3)
                );

        given(photoStorageService.store(file, "hotels/100/room-types/10"))
                .willReturn("hotels/100/room-types/10/photo.jpg");

        when(roomTypePhotoRepository.save(any(RoomTypePhoto.class)))
                .thenReturn(savedPhoto);

        RoomTypePhoto result = service.addPhoto(hotelId, roomTypeId, file, "caption", 2);

        assertEquals(2, result.getSortOrder());
        assertEquals(1, existing1.getSortOrder());
        assertEquals(3, existing2.getSortOrder());
        assertEquals(4, existing3.getSortOrder());
    }

    @Test
    @DisplayName("addPhoto -> should append at end when requested sortOrder is greater than max position")
    void addPhoto_WhenSortOrderTooLarge_ShouldAppendAtEnd() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        RoomTypePhoto existing1 = buildPhoto(1L, roomTypeId, "a.jpg", 1, true);
        RoomTypePhoto existing2 = buildPhoto(2L, roomTypeId, "b.jpg", 2, false);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        RoomTypePhoto savedPhoto = new RoomTypePhoto();
        savedPhoto.setId(3L);
        savedPhoto.setRoomType(buildRoomType(roomTypeId, hotelId));
        savedPhoto.setStorageKey("hotels/100/room-types/10/photo.jpg");
        savedPhoto.setCaption("caption");
        savedPhoto.setSortOrder(3);
        savedPhoto.setPrimary(false);

        given(roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId))
                .willReturn(
                        List.of(existing1, existing2),
                        List.of(existing1, existing2, savedPhoto)
                );

        given(photoStorageService.store(file, "hotels/100/room-types/10"))
                .willReturn("hotels/100/room-types/10/photo.jpg");

        when(roomTypePhotoRepository.save(any(RoomTypePhoto.class)))
                .thenReturn(savedPhoto);

        RoomTypePhoto result = service.addPhoto(hotelId, roomTypeId, file, "caption", 99);

        assertEquals(3, result.getSortOrder());
        assertEquals(1, existing1.getSortOrder());
        assertEquals(2, existing2.getSortOrder());
    }

    @Test
    @DisplayName("addPhoto -> should throw when sortOrder is zero")
    void addPhoto_WhenSortOrderIsZero_ShouldThrow() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1}
        );

        assertThrows(InvalidPhotoOrderException.class,
                () -> service.addPhoto(hotelId, roomTypeId, file, "caption", 0));
    }

    @Test
    @DisplayName("addPhoto -> should throw when sortOrder is negative")
    void addPhoto_WhenSortOrderIsNegative_ShouldThrow() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1}
        );

        assertThrows(InvalidPhotoOrderException.class,
                () -> service.addPhoto(hotelId, roomTypeId, file, "caption", -2));
    }

    @Test
    @DisplayName("addPhoto -> should throw when hotel not found")
    void addPhoto_WhenHotelNotFound_ShouldThrow() {
        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1}
        );

        assertThrows(HotelNotFoundException.class,
                () -> service.addPhoto(100L, 10L, file, "caption", null));
    }

    @Test
    @DisplayName("addPhoto -> should throw when room type not found")
    void addPhoto_WhenRoomTypeNotFound_ShouldThrow() {
        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(buildHotel(100L)));
        given(roomTypeRepository.findByIdAndHotelIdAndHotelNotDeleted(10L, 100L))
                .willReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1}
        );

        assertThrows(RoomTypeNotFoundException.class,
                () -> service.addPhoto(100L, 10L, file, "caption", null));
    }

    @Test
    @DisplayName("listPhotos -> should return ordered photos")
    void listPhotos_ShouldReturnPhotos() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        List<RoomTypePhoto> photos = List.of(
                buildPhoto(1L, roomTypeId, "a.jpg", 1, true),
                buildPhoto(2L, roomTypeId, "b.jpg", 2, false)
        );

        given(roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId))
                .willReturn(photos);

        List<RoomTypePhoto> result = service.listPhotos(hotelId, roomTypeId);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    @DisplayName("listPhotos -> should throw when room type not found")
    void listPhotos_WhenRoomTypeNotFound_ShouldThrow() {
        given(hotelRepository.findByIdAndStatusAndIsDeletedFalse(100L, APPROVED))
                .willReturn(Optional.of(buildHotel(100L)));
        given(roomTypeRepository.findByIdAndHotelIdAndHotelNotDeleted(10L, 100L))
                .willReturn(Optional.empty());

        assertThrows(RoomTypeNotFoundException.class,
                () -> service.listPhotos(100L, 10L));
    }

    @Test
    @DisplayName("deletePhoto -> should delete photo and storage object")
    void deletePhoto_ShouldDeletePhotoAndStorage() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;
        Long photoId = 1L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        RoomTypePhoto photo = buildPhoto(photoId, roomTypeId, "a.jpg", 1, false);

        given(roomTypePhotoRepository.findByIdAndRoomTypeId(photoId, roomTypeId))
                .willReturn(Optional.of(photo));
        given(roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId))
                .willReturn(List.of());

        service.deletePhoto(hotelId, roomTypeId, photoId);

        verify(roomTypePhotoRepository, times(1)).delete(photo);
        verify(photoStorageService, times(1)).delete("a.jpg");
    }

    @Test
    @DisplayName("deletePhoto -> when primary photo deleted should set first remaining as primary and normalize sort")
    void deletePhoto_WhenPrimaryDeleted_ShouldPromoteFirstRemaining() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;
        Long photoId = 1L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        RoomTypePhoto primary = buildPhoto(photoId, roomTypeId, "a.jpg", 1, true);
        RoomTypePhoto remaining1 = buildPhoto(2L, roomTypeId, "b.jpg", 5, false);
        RoomTypePhoto remaining2 = buildPhoto(3L, roomTypeId, "c.jpg", 7, false);

        given(roomTypePhotoRepository.findByIdAndRoomTypeId(photoId, roomTypeId))
                .willReturn(Optional.of(primary));
        given(roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId))
                .willReturn(List.of(remaining1, remaining2));

        service.deletePhoto(hotelId, roomTypeId, photoId);

        assertTrue(remaining1.isPrimary());
        assertFalse(remaining2.isPrimary());
        assertEquals(1, remaining1.getSortOrder());
        assertEquals(2, remaining2.getSortOrder());
    }

    @Test
    @DisplayName("deletePhoto -> should throw when photo not found")
    void deletePhoto_WhenPhotoNotFound_ShouldThrow() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;
        Long photoId = 1L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        given(roomTypePhotoRepository.findByIdAndRoomTypeId(photoId, roomTypeId))
                .willReturn(Optional.empty());

        assertThrows(RoomTypePhotoNotFoundException.class,
                () -> service.deletePhoto(hotelId, roomTypeId, photoId));
    }

    @Test
    @DisplayName("setPrimaryPhoto -> should set selected photo as primary and others false")
    void setPrimaryPhoto_ShouldSetOnlyTargetAsPrimary() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;
        Long photoId = 2L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        RoomTypePhoto p1 = buildPhoto(1L, roomTypeId, "a.jpg", 1, true);
        RoomTypePhoto p2 = buildPhoto(2L, roomTypeId, "b.jpg", 2, false);
        RoomTypePhoto p3 = buildPhoto(3L, roomTypeId, "c.jpg", 3, false);

        given(roomTypePhotoRepository.findByIdAndRoomTypeId(photoId, roomTypeId))
                .willReturn(Optional.of(p2));
        given(roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId))
                .willReturn(List.of(p1, p2, p3));

        service.setPrimaryPhoto(hotelId, roomTypeId, photoId);

        assertFalse(p1.isPrimary());
        assertTrue(p2.isPrimary());
        assertFalse(p3.isPrimary());
    }

    @Test
    @DisplayName("setPrimaryPhoto -> should throw when target photo not found")
    void setPrimaryPhoto_WhenPhotoNotFound_ShouldThrow() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;
        Long photoId = 2L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        given(roomTypePhotoRepository.findByIdAndRoomTypeId(photoId, roomTypeId))
                .willReturn(Optional.empty());

        assertThrows(RoomTypePhotoNotFoundException.class,
                () -> service.setPrimaryPhoto(hotelId, roomTypeId, photoId));
    }

    @Test
    @DisplayName("reorderPhotos -> should update sort orders")
    void reorderPhotos_ShouldUpdateSortOrders() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        RoomTypePhoto p1 = buildPhoto(1L, roomTypeId, "a.jpg", 1, true);
        RoomTypePhoto p2 = buildPhoto(2L, roomTypeId, "b.jpg", 2, false);
        RoomTypePhoto p3 = buildPhoto(3L, roomTypeId, "c.jpg", 3, false);

        given(roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId))
                .willReturn(List.of(p1, p2, p3));

        service.reorderPhotos(hotelId, roomTypeId, List.of(3L, 1L, 2L));

        assertEquals(2, p1.getSortOrder());
        assertEquals(3, p2.getSortOrder());
        assertEquals(1, p3.getSortOrder());
    }

    @Test
    @DisplayName("reorderPhotos -> should throw when not all photos included")
    void reorderPhotos_WhenMissingPhotos_ShouldThrow() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        RoomTypePhoto p1 = buildPhoto(1L, roomTypeId, "a.jpg", 1, true);
        RoomTypePhoto p2 = buildPhoto(2L, roomTypeId, "b.jpg", 2, false);

        given(roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId))
                .willReturn(List.of(p1, p2));

        assertThrows(InvalidPhotoOrderException.class,
                () -> service.reorderPhotos(hotelId, roomTypeId, List.of(1L)));
    }

    @Test
    @DisplayName("reorderPhotos -> should throw when duplicate ids exist")
    void reorderPhotos_WhenDuplicateIds_ShouldThrow() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        RoomTypePhoto p1 = buildPhoto(1L, roomTypeId, "a.jpg", 1, true);
        RoomTypePhoto p2 = buildPhoto(2L, roomTypeId, "b.jpg", 2, false);

        given(roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId))
                .willReturn(List.of(p1, p2));

        assertThrows(InvalidPhotoOrderException.class,
                () -> service.reorderPhotos(hotelId, roomTypeId, List.of(1L, 1L)));
    }

    @Test
    @DisplayName("reorderPhotos -> should throw when photo id does not belong to room type")
    void reorderPhotos_WhenForeignPhotoId_ShouldThrow() {
        Long hotelId = 100L;
        Long roomTypeId = 10L;

        mockValidHotelAndRoomType(hotelId, roomTypeId);

        RoomTypePhoto p1 = buildPhoto(1L, roomTypeId, "a.jpg", 1, true);
        RoomTypePhoto p2 = buildPhoto(2L, roomTypeId, "b.jpg", 2, false);

        given(roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId))
                .willReturn(List.of(p1, p2));

        assertThrows(InvalidPhotoOrderException.class,
                () -> service.reorderPhotos(hotelId, roomTypeId, List.of(1L, 99L)));
    }
}
