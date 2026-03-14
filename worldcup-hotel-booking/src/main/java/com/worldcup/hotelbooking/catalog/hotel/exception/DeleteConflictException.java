package com.worldcup.hotelbooking.catalog.hotel.exception;

public class DeleteConflictException extends RuntimeException {
    public DeleteConflictException(Long id) {
        super("Hotel with id " + id + " cannot be deleted because it has active bookings.");
    }
}
