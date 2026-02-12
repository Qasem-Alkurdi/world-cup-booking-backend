package com.worldcup.hotelbooking.catalog.hotel.exceptions;

public class DeleteConflictException extends RuntimeException {
    public DeleteConflictException(Long id) {
        super("Hotel with id " + id + " cannot be deleted because it has active bookings.");
    }
}
