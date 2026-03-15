package com.worldcup.hotelbooking.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ProcessPaymentResponseDto {
    private Long id;
    private String paymentIntentId;
    private String transactionReference;

    private Long bookingId;
    private String bookingReference;

    private BigDecimal totalPaidAmount;
    private String currency;

    private Payment.PaymentStatus status;
    private Payment.PaymentMethod paymentMethod;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    // If failed
    private String failureReason;


}
