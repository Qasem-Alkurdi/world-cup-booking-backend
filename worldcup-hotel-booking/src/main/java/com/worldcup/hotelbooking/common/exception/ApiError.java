package com.worldcup.hotelbooking.common.exception;

import lombok.Data;

@Data
public class ApiError  {
    private final String timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    public ApiError(String timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
    }

