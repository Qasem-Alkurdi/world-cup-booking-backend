package com.worldcup.hotelbooking.availability_pricing.stadium;

public class StadiumNotFoundException extends RuntimeException {
    public StadiumNotFoundException(String message) {
        super(message);
    }
}