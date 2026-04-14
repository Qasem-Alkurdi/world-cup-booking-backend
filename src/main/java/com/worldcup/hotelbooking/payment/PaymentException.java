package com.worldcup.hotelbooking.payment;

public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
