package com.worldcup.hotelbooking.user.user;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("user with id " + id + " not found");
    }
}
