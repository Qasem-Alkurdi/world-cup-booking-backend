package com.worldcup.hotelbooking.catalog.hotel.maper;

public class HotelNotFoundException extends RuntimeException {
    public HotelNotFoundException(Long id ) {
        super("Could not found Hotel "+ id );
    }
}
