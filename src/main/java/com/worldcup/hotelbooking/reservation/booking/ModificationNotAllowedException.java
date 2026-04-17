package com.worldcup.hotelbooking.reservation.booking;

public class ModificationNotAllowedException extends RuntimeException {
    public ModificationNotAllowedException(String message) {
        super(message);
    }
}
