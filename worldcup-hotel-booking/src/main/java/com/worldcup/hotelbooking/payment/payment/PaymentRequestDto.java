package com.worldcup.hotelbooking.payment.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDto {

    @NotNull
    private Long bookingId;

    @NotBlank(message = "Amount cannot be blank")
    private Payment.PaymentMethod paymentMethod;
}

