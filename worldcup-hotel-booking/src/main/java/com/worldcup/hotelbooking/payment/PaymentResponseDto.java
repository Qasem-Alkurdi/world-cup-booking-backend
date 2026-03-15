package com.worldcup.hotelbooking.payment;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment response (after processing)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private Long id;
    private String paymentIntentId;
    private String transactionReference;

    private Long bookingId;
    private String bookingReference;

    private BigDecimal totalAmount_paidAmountWithAdditionalPaymentWithoutRefund;
    private String currency;

    private Payment.PaymentStatus status;
    private Payment.PaymentMethod paymentMethod;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private BigDecimal totalPaidAmount;

    // If failed
    private String failureReason;

    // If refunded
    private BigDecimal refundAmount;
    private LocalDateTime refundedAt;
    private String refundReason;

        // If additional payment
    private BigDecimal requiredAdditionalPaymentAmount;
    private BigDecimal paidAdditionalPaymentAmount;
    private LocalDateTime payAdditionalPaymentAt;

}