package com.worldcup.hotelbooking.booking.booking;

public class ModificationNotAllowedException extends RuntimeException {
    public ModificationNotAllowedException(String message) {
        super(message);
    }
}
