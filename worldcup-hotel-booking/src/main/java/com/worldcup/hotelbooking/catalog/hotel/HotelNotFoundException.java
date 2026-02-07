package com.worldcup.hotelbooking.catalog.hotel;

public class HotelNotFoundException extends RuntimeException {
    public HotelNotFoundException(String message) {
        super(message);
    }
}
