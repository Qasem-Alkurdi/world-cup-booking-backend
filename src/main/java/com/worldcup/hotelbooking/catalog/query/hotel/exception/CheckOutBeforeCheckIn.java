package com.worldcup.hotelbooking.catalog.query.hotel.exception;

public class CheckOutBeforeCheckIn extends IllegalArgumentException {
    public CheckOutBeforeCheckIn(String message) {
        super(message);
    }

    public CheckOutBeforeCheckIn() {
        super("Check-out date cannot be before check-in date.");
    }
}
