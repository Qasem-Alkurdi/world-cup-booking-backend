package com.worldcup.hotelbooking.user.user;

public class AppUserNotFoundException extends RuntimeException {
        public AppUserNotFoundException(String message) {
            super(message);
        }
}
