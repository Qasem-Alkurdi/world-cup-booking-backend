package com.worldcup.hotelbooking.payment.payment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponseDto {

    private Long paymentId;
    private String bookingReference;

    private BigDecimal amount;
    private String currency;

    private String paymentMethod;
    private String status;

    private String transactionReference;

    private LocalDateTime paidAt;
}
