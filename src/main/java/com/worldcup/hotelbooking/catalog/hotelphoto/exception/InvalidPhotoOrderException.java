package com.worldcup.hotelbooking.catalog.hotelphoto.exception;

import com.worldcup.hotelbooking.common.exception.ApiException;

public class InvalidPhotoOrderException extends ApiException {
    public InvalidPhotoOrderException(String message) {
        super(message);
    }
}