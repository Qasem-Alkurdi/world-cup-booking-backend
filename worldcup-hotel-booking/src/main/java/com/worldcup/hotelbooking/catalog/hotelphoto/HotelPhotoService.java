package com.worldcup.hotelbooking.catalog.hotelphoto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HotelPhotoService {
    HotelPhoto addPhoto(Long hotelId, MultipartFile file, String caption, Integer sortOrder);

    List<HotelPhoto> listPhotos(Long hotelId);

    void deletePhoto(Long hotelId, Long photoId);

    void setPrimaryPhoto(Long hotelId, Long photoId);

    void reorderPhotos(Long hotelId, List<Long> photoIds);
}