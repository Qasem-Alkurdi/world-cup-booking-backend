package com.worldcup.hotelbooking.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds
) {
    public LoginResponse(String accessToken, String refreshToken, long expiresInSeconds) {
        this(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}