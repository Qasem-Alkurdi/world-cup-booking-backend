package com.worldcup.hotelbooking.catalog.roomtypephoto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RoomTypePhotoService {
    RoomTypePhoto addPhoto(Long hotelId, Long roomTypeId, MultipartFile file, String caption, Integer sortOrder);

    List<RoomTypePhoto> listPhotos(Long hotelId, Long roomTypeId);

    void deletePhoto(Long hotelId, Long roomTypeId, Long photoId);

    void setPrimaryPhoto(Long hotelId, Long roomTypeId, Long photoId);

    void reorderPhotos(Long hotelId, Long roomTypeId, List<Long> photoIds);
}