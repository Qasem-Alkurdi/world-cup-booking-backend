package com.worldcup.hotelbooking.catalog.roomtypephoto.exception;

import com.worldcup.hotelbooking.common.exception.ApiException;

public class RoomTypePhotoNotFoundException extends ApiException {

    public RoomTypePhotoNotFoundException(Long hotelId, Long roomTypeId, Long photoId) {
        super(
                "Photo not found for hotelId=" + hotelId +
                        ", roomTypeId=" + roomTypeId +
                        ", photoId=" + photoId
        );
    }
}