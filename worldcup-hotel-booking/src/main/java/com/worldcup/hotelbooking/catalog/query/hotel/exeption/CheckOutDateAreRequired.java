package com.worldcup.hotelbooking.catalog.query.hotel.exeption;

public class CheckOutDateAreRequired extends IllegalArgumentException {
    public CheckOutDateAreRequired(String message) {
        super(message);
    }

    public CheckOutDateAreRequired() {
        super("checkOutDate are required ");
    }
}
