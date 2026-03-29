// com.worldcup.hotelbooking.user.user.PasswordValidationException.java
package com.worldcup.hotelbooking.user;

import java.util.List;

public class PasswordValidationException extends RuntimeException {
    private final List<String> errors;

    public PasswordValidationException(List<String> errors) {
        super("Password validation failed: " + String.join("; ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}