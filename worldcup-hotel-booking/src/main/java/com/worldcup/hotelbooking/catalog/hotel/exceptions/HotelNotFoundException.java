package com.worldcup.hotelbooking.catalog.hotel.exceptions;

public class HotelNotFoundException extends RuntimeException {
    public HotelNotFoundException(Long id) {
        super("Hotel with id " + id + " was not found.");
    }
}
