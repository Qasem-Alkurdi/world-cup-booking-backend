package com.worldcup.hotelbooking.catalog.hotel.exception;

public class HotelNotFoundException extends RuntimeException {
    public HotelNotFoundException(Long id) {
        super("Hotel with id " + id + " was not found.");
    }
}
