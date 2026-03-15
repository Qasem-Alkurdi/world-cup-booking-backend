package com.worldcup.hotelbooking.catalog.hotelphoto;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotel.exception.HotelNotFoundException;
import com.worldcup.hotelbooking.catalog.hotelphoto.exception.HotelPhotoNotFoundException;
import com.worldcup.hotelbooking.catalog.hotelphoto.exception.InvalidPhotoOrderException;
import com.worldcup.hotelbooking.catalog.storage.PhotoStorageService;
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
public class HotelPhotoServiceImpl implements HotelPhotoService {

    private final HotelPhotoRepository hotelPhotoRepository;
    private final HotelRepository hotelRepository;
    private final PhotoStorageService photoStorageService;

    public HotelPhotoServiceImpl(
            HotelPhotoRepository hotelPhotoRepository,
            HotelRepository hotelRepository,
            PhotoStorageService photoStorageService
    ) {
        this.hotelPhotoRepository = hotelPhotoRepository;
        this.hotelRepository = hotelRepository;
        this.photoStorageService = photoStorageService;
    }

    private Hotel getActiveApprovedHotel(Long hotelId) {
        return hotelRepository.findByIdAndStatusAndIsDeletedFalse(hotelId, APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));
    }

    private void validateSortOrder(Integer sortOrder) {
        if (sortOrder != null && sortOrder <= 0) {
            throw new InvalidPhotoOrderException("sortOrder must be greater than 0");
        }
    }

    private void normalizeSortOrders(Long hotelId) {
        List<HotelPhoto> photos = hotelPhotoRepository.findByHotelIdOrderBySortOrderAscCreatedAtAsc(hotelId);

        for (int i = 0; i < photos.size(); i++) {
            photos.get(i).setSortOrder(i + 1);
        }
    }

    @Override
    public HotelPhoto addPhoto(Long hotelId, MultipartFile file, String caption, Integer sortOrder) {
        Hotel hotel = getActiveApprovedHotel(hotelId);
        validateSortOrder(sortOrder);

        List<HotelPhoto> existingPhotos = hotelPhotoRepository.findByHotelIdOrderBySortOrderAscCreatedAtAsc(hotelId);

        int finalSortOrder;
        if (sortOrder == null) {
            finalSortOrder = existingPhotos.size() + 1;
        } else {
            finalSortOrder = Math.min(sortOrder, existingPhotos.size() + 1);

            for (HotelPhoto existingPhoto : existingPhotos) {
                if (existingPhoto.getSortOrder() >= finalSortOrder) {
                    existingPhoto.setSortOrder(existingPhoto.getSortOrder() + 1);
                }
            }
        }

        String storageKey = photoStorageService.store(file, "hotels/" + hotelId);

        boolean hasPrimary = existingPhotos.stream().anyMatch(HotelPhoto::isPrimary);

        HotelPhoto photo = new HotelPhoto(hotel, storageKey, caption, finalSortOrder);
        photo.setPrimary(!hasPrimary);

        HotelPhoto saved = hotelPhotoRepository.save(photo);

        normalizeSortOrders(hotelId);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelPhoto> listPhotos(Long hotelId) {
        getActiveApprovedHotel(hotelId);
        return hotelPhotoRepository.findByHotelIdOrderBySortOrderAscCreatedAtAsc(hotelId);
    }

    @Override
    public void deletePhoto(Long hotelId, Long photoId) {
        getActiveApprovedHotel(hotelId);

        HotelPhoto photo = hotelPhotoRepository.findByIdAndHotelId(photoId, hotelId)
                .orElseThrow(() -> new HotelPhotoNotFoundException(hotelId, photoId));

        boolean wasPrimary = photo.isPrimary();
        String storageKey = photo.getStorageKey();

        hotelPhotoRepository.delete(photo);
        photoStorageService.delete(storageKey);

        normalizeSortOrders(hotelId);

        if (wasPrimary) {
            List<HotelPhoto> remaining = hotelPhotoRepository.findByHotelIdOrderBySortOrderAscCreatedAtAsc(hotelId);
            if (!remaining.isEmpty()) {
                for (HotelPhoto p : remaining) {
                    p.setPrimary(false);
                }
                remaining.get(0).setPrimary(true);
            }
        }
    }

    @Override
    public void setPrimaryPhoto(Long hotelId, Long photoId) {
        getActiveApprovedHotel(hotelId);

        HotelPhoto target = hotelPhotoRepository.findByIdAndHotelId(photoId, hotelId)
                .orElseThrow(() -> new HotelPhotoNotFoundException(hotelId, photoId));

        List<HotelPhoto> photos = hotelPhotoRepository.findByHotelIdOrderBySortOrderAscCreatedAtAsc(hotelId);

        for (HotelPhoto photo : photos) {
            photo.setPrimary(photo.getId().equals(target.getId()));
        }
    }

    @Override
    public void reorderPhotos(Long hotelId, List<Long> photoIds) {
        getActiveApprovedHotel(hotelId);

        List<HotelPhoto> photos = hotelPhotoRepository.findByHotelIdOrderBySortOrderAscCreatedAtAsc(hotelId);

        if (photos.size() != photoIds.size()) {
            throw new InvalidPhotoOrderException("All hotel photos must be included in reorder request");
        }

        if (new HashSet<>(photoIds).size() != photoIds.size()) {
            throw new InvalidPhotoOrderException("Duplicate photo ids are not allowed");
        }

        Map<Long, HotelPhoto> photoMap = photos.stream()
                .collect(Collectors.toMap(HotelPhoto::getId, p -> p));

        for (Long photoId : photoIds) {
            if (!photoMap.containsKey(photoId)) {
                throw new InvalidPhotoOrderException(
                        "Photo id " + photoId + " does not belong to hotel " + hotelId
                );
            }
        }

        for (int i = 0; i < photoIds.size(); i++) {
            HotelPhoto photo = photoMap.get(photoIds.get(i));
            photo.setSortOrder(i + 1);
        }
    }
}
