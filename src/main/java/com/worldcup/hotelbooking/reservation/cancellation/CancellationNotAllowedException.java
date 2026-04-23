package com.worldcup.hotelbooking.reservation.cancellation;

public class CancellationNotAllowedException extends RuntimeException {
    public CancellationNotAllowedException(String message) {
        super(message);
    }
}
