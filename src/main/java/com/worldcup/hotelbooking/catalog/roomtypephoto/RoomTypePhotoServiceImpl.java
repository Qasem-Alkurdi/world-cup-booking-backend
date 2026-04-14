package com.worldcup.hotelbooking.catalog.roomtypephoto;

import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotel.exception.HotelNotFoundException;
import com.worldcup.hotelbooking.catalog.hotelphoto.exception.InvalidPhotoOrderException;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import com.worldcup.hotelbooking.catalog.roomtype.exception.RoomTypeNotFoundException;
import com.worldcup.hotelbooking.catalog.roomtypephoto.exception.RoomTypePhotoNotFoundException;
import com.worldcup.hotelbooking.catalog.storage.PhotoStorageService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.worldcup.hotelbooking.catalog.hotel.HotelStatus.APPROVED;

@Service
@Transactional
public class RoomTypePhotoServiceImpl implements RoomTypePhotoService {

    private final RoomTypePhotoRepository roomTypePhotoRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final HotelRepository hotelRepository;
    private final PhotoStorageService photoStorageService;

    public RoomTypePhotoServiceImpl(
            RoomTypePhotoRepository roomTypePhotoRepository,
            RoomTypeRepository roomTypeRepository,
            HotelRepository hotelRepository,
            PhotoStorageService photoStorageService
    ) {
        this.roomTypePhotoRepository = roomTypePhotoRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.hotelRepository = hotelRepository;
        this.photoStorageService = photoStorageService;
    }

    private void validateHotel(Long hotelId) {
        hotelRepository.findByIdAndStatusAndIsDeletedFalse(hotelId, APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));
    }

    private RoomType getRoomTypeOrThrow(Long hotelId, Long roomTypeId) {
        return roomTypeRepository.findByIdAndHotelIdAndHotelNotDeleted(roomTypeId, hotelId)
                .orElseThrow(() -> new RoomTypeNotFoundException(hotelId, roomTypeId));
    }

    private void validateSortOrder(Integer sortOrder) {
        if (sortOrder != null && sortOrder <= 0) {
            throw new InvalidPhotoOrderException("sortOrder must be greater than 0");
        }
    }

    private void normalizeSortOrders(Long roomTypeId) {
        List<RoomTypePhoto> photos =
                roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId);

        for (int i = 0; i < photos.size(); i++) {
            photos.get(i).setSortOrder(i + 1);
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "hotelById", key = "#id"),
            @CacheEvict(value = "hotelList", allEntries = true),
            @CacheEvict(value = "myHotels", allEntries = true),
            @CacheEvict(value = "hotelPhotos", key = "#id"),
            @CacheEvict(value = "roomTypesByHotel", key = "#id"),
            @CacheEvict(value = "roomTypeById", allEntries = true),
            @CacheEvict(value = "roomTypePhotos", allEntries = true)
    })
    @Override
    public RoomTypePhoto addPhoto(Long hotelId, Long roomTypeId, MultipartFile file, String caption, Integer sortOrder) {
        validateHotel(hotelId);
        RoomType roomType = getRoomTypeOrThrow(hotelId, roomTypeId);
        validateSortOrder(sortOrder);

        List<RoomTypePhoto> existingPhotos =
                roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId);

        int finalSortOrder;
        if (sortOrder == null) {
            finalSortOrder = existingPhotos.size() + 1;
        } else {
            finalSortOrder = Math.min(sortOrder, existingPhotos.size() + 1);

            for (RoomTypePhoto existingPhoto : existingPhotos) {
                if (existingPhoto.getSortOrder() >= finalSortOrder) {
                    existingPhoto.setSortOrder(existingPhoto.getSortOrder() + 1);
                }
            }
        }

        String storageKey = photoStorageService.store(file, "hotels/" + hotelId + "/room-types/" + roomTypeId);

        boolean hasPrimary = existingPhotos.stream().anyMatch(RoomTypePhoto::isPrimary);

        RoomTypePhoto photo = new RoomTypePhoto(roomType, storageKey, caption, finalSortOrder);
        photo.setPrimary(!hasPrimary);

        RoomTypePhoto saved = roomTypePhotoRepository.save(photo);

        normalizeSortOrders(roomTypeId);

        return saved;
    }


    /**
     * Returns the ordered photo list for a room type.
     * Cached by composite key hotelId + roomTypeId — evicted by any mutation to this
     * room type's photos.
     */
    @Override
    @Cacheable(value = "roomTypePhotos", key = "#hotelId + '_' + #roomTypeId")
    @Transactional(readOnly = true)
    public List<RoomTypePhoto> listPhotos(Long hotelId, Long roomTypeId) {
        validateHotel(hotelId);
        getRoomTypeOrThrow(hotelId, roomTypeId);

        return roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId);
    }

    @Caching(evict = {
            @CacheEvict(value = "hotelById", key = "#id"),
            @CacheEvict(value = "hotelList", allEntries = true),
            @CacheEvict(value = "myHotels", allEntries = true),
            @CacheEvict(value = "hotelPhotos", key = "#id"),
            @CacheEvict(value = "roomTypesByHotel", key = "#id"),
            @CacheEvict(value = "roomTypeById", allEntries = true),
            @CacheEvict(value = "roomTypePhotos", allEntries = true)
    })
    @Override
    public void deletePhoto(Long hotelId, Long roomTypeId, Long photoId) {
        validateHotel(hotelId);
        getRoomTypeOrThrow(hotelId, roomTypeId);

        RoomTypePhoto photo = roomTypePhotoRepository.findByIdAndRoomTypeId(photoId, roomTypeId)
                .orElseThrow(() -> new RoomTypePhotoNotFoundException(hotelId, roomTypeId, photoId));

        boolean wasPrimary = photo.isPrimary();
        String storageKey = photo.getStorageKey();

        roomTypePhotoRepository.delete(photo);
        photoStorageService.delete(storageKey);

        normalizeSortOrders(roomTypeId);

        if (wasPrimary) {
            List<RoomTypePhoto> remaining =
                    roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId);

            if (!remaining.isEmpty()) {
                for (RoomTypePhoto p : remaining) {
                    p.setPrimary(false);
                }
                remaining.get(0).setPrimary(true);
            }
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "hotelById", key = "#id"),
            @CacheEvict(value = "hotelList", allEntries = true),
            @CacheEvict(value = "myHotels", allEntries = true),
            @CacheEvict(value = "hotelPhotos", key = "#id"),
            @CacheEvict(value = "roomTypesByHotel", key = "#id"),
            @CacheEvict(value = "roomTypeById", allEntries = true),
            @CacheEvict(value = "roomTypePhotos", allEntries = true)
    })
    @Override
    public void setPrimaryPhoto(Long hotelId, Long roomTypeId, Long photoId) {
        validateHotel(hotelId);
        getRoomTypeOrThrow(hotelId, roomTypeId);

        RoomTypePhoto target = roomTypePhotoRepository.findByIdAndRoomTypeId(photoId, roomTypeId)
                .orElseThrow(() -> new RoomTypePhotoNotFoundException(hotelId, roomTypeId, photoId));

        List<RoomTypePhoto> photos =
                roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId);

        for (RoomTypePhoto photo : photos) {
            photo.setPrimary(photo.getId().equals(target.getId()));
        }
    }

    /**
     * Reordering changes display sequence only — the primary photo does not change,
     * so roomTypeById and roomTypesByHotel are NOT affected.
     * Only the ordered photo list (roomTypePhotos) needs eviction.
     */
    @Override
    @CacheEvict(value = "roomTypePhotos", key = "#hotelId + '_' + #roomTypeId")
    public void reorderPhotos(Long hotelId, Long roomTypeId, List<Long> photoIds) {
        validateHotel(hotelId);
        getRoomTypeOrThrow(hotelId, roomTypeId);

        List<RoomTypePhoto> photos =
                roomTypePhotoRepository.findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(roomTypeId);

        if (photos.size() != photoIds.size()) {
            throw new InvalidPhotoOrderException("All room type photos must be included in reorder request");
        }

        if (new HashSet<>(photoIds).size() != photoIds.size()) {
            throw new InvalidPhotoOrderException("Duplicate photo ids are not allowed");
        }

        Map<Long, RoomTypePhoto> photoMap = photos.stream()
                .collect(Collectors.toMap(RoomTypePhoto::getId, p -> p));

        for (Long photoId : photoIds) {
            if (!photoMap.containsKey(photoId)) {
                throw new InvalidPhotoOrderException(
                        "Photo id " + photoId + " does not belong to roomType " + roomTypeId
                );
            }
        }

        for (int i = 0; i < photoIds.size(); i++) {
            RoomTypePhoto photo = photoMap.get(photoIds.get(i));
            photo.setSortOrder(i + 1);
        }
    }
}
