package com.worldcup.hotelbooking.booking.core;

public class ModificationNotAllowedException extends RuntimeException {
    public ModificationNotAllowedException(String message) {
        super(message);
    }
}
