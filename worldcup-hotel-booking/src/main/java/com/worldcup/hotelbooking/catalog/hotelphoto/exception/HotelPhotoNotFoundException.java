package com.worldcup.hotelbooking.catalog.hotelphoto.exception;

import com.worldcup.hotelbooking.common.exception.ApiException;

public class HotelPhotoNotFoundException extends ApiException {

    public HotelPhotoNotFoundException(Long hotelId, Long photoId) {
        super("Photo not found for hotelId=" + hotelId + ", photoId=" + photoId);
    }
}