package com.worldcup.hotelbooking.payment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

import javax.management.ConstructorParameters;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ========================================
// RESPONSE DTOs
// ========================================

/**
 * Payment intent response (after creating intent)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({
        "paymentIntentId",
        "paymentId",
        "bookingId",
        "bookingReference",
        "amount",
        "currency",
        "status",
        "paymentMethod",
        "createdAt",
        "clientSecret",
        "nextAction"
})//this annotation specifies the order in which the properties of the PaymentIntentResponseDto class will be serialized to JSON. When an instance of this class is converted to JSON, the properties will appear in the specified order: paymentIntentId, paymentId, bookingId, bookingReference, amount, currency, status, paymentMethod, createdAt, clientSecret, and nextAction.
public class PaymentIntentResponseDto {

    private String paymentIntentId;
    private Long paymentId;
    private Long bookingId;
    private String bookingReference;

    private BigDecimal amount;
    private String currency;

    private Payment.PaymentStatus status;
    private Payment.PaymentMethod paymentMethod;

    private LocalDateTime createdAt;

    // For frontend to complete payment
    private String clientSecret;  // Mock: just return intent ID
    private String nextAction;    // e.g., "redirect_to_url", "complete_payment"


    public PaymentIntentResponseDto(String paymentIntentId, Long paymentId, Long bookingId, String bookingReference,
                                    BigDecimal amount, String currency, Payment.PaymentStatus status,
                                    Payment.PaymentMethod paymentMethod, LocalDateTime createdAt) {
        this.paymentIntentId = paymentIntentId;
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.bookingReference = bookingReference;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
        this.clientSecret = paymentIntentId; // For mock, use intent ID as client secret
        this.nextAction = "complete_payment"; // For mock, assume we just complete payment
    }

}