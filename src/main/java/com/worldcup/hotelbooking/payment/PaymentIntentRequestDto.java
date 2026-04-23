package com.worldcup.hotelbooking.payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ========================================
// REQUEST DTOs
// ========================================

/**
 * Request to create a payment intent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentRequestDto {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Payment method is required")
    private Payment.PaymentMethod paymentMethod;

    private String returnUrl;  // For redirecting after payment
}

