package com.worldcup.hotelbooking.reservation.bookingroom;

public class BookingRoomNotFoundException extends RuntimeException {
    public BookingRoomNotFoundException(String message) {
        super(message);
    }
}
