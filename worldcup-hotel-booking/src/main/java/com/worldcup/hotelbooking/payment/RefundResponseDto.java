package com.worldcup.hotelbooking.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponseDto {

    private Long paymentId;
    private String transactionReference;

    private BigDecimal originalAmount;
    private BigDecimal refundAmount;
    private BigDecimal remainingAmount;


    private Payment.PaymentStatus newStatus;

    private LocalDateTime refundedAt;
    private String refundReason;

    private boolean success;
    private String message;


}

