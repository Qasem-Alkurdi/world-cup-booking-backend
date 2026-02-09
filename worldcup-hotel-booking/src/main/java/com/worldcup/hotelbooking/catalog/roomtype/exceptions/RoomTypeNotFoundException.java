package com.worldcup.hotelbooking.catalog.roomtype.exceptions;

public class RoomTypeNotFoundException extends RuntimeException {
    public RoomTypeNotFoundException(Long hotelId, Long roomTypeId) {
        super("Could not found RoomType " + roomTypeId + " for Hotel " + hotelId);
    }
}
