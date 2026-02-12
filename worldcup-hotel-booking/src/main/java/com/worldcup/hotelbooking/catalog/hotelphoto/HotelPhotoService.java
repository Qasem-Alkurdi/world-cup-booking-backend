package com.worldcup.hotelbooking.catalog.hotelphoto;

import java.util.List;

public interface HotelPhotoService {
    HotelPhoto addPhoto(Long hotelId, String storageKey, String caption, Integer sortOrder);

    List<HotelPhoto> listPhotos(Long hotelId);

    void deletePhoto(Long hotelId, Long photoId);
}
