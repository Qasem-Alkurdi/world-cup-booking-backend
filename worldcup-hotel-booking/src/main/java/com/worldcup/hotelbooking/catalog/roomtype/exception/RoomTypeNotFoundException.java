package com.worldcup.hotelbooking.catalog.roomtype.exception;

public class RoomTypeNotFoundException extends RuntimeException {

    public RoomTypeNotFoundException(Long roomTypeId) {
        super("Room type not found with id: " + roomTypeId);
    }

    public RoomTypeNotFoundException(Long hotelId, Long roomTypeId) {
        super("Room type not found with id: " + roomTypeId + " for hotel id: " + hotelId);
    }
}
