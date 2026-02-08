package com.worldcup.hotelbooking.payment.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDto {

    @NotBlank(message = "Booking ID cannot be blank")
    private Long bookingId;

    @NotBlank(message = "Amount cannot be blank")
    private String paymentMethod;
}

