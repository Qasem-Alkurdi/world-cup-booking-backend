package com.worldcup.hotelbooking.tournament.stadium;

public class StadiumNotFoundException extends RuntimeException {
    public StadiumNotFoundException(String message) {
        super(message);
    }
}