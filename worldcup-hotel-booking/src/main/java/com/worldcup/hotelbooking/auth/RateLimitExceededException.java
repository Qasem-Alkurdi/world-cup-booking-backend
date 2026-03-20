package com.worldcup.hotelbooking.auth;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super("Too many requests, please try again later");
    }
}
