package com.worldcup.hotelbooking.review.exception;

public class ReviewOwnershipException extends RuntimeException {

    public ReviewOwnershipException() {
        super("You are not allowed to manage this review");
    }

    public ReviewOwnershipException(String message) {
        super(message);
    }
}