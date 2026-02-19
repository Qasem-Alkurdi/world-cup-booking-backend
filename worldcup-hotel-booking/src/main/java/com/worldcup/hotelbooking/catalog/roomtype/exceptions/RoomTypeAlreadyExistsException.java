package com.worldcup.hotelbooking.catalog.roomtype.exceptions;

public class RoomTypeAlreadyExistsException extends RuntimeException {
    public RoomTypeAlreadyExistsException(Long hotelId, String name) {
        super("RoomType with name '" + name + "' already exists for Hotel " + hotelId);
    }
}
