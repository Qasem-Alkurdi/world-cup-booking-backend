package com.worldcup.hotelbooking.review.exception;

public class ReviewAlreadyExistsException extends RuntimeException {
    public ReviewAlreadyExistsException(Long bookingId) {
        super("A review already exists for booking id: " + bookingId);
    }
}