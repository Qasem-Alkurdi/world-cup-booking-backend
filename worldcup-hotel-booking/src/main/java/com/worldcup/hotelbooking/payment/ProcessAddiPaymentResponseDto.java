package com.worldcup.hotelbooking.payment;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({
        "id",
        "paymentIntentId",
        "transactionReference",
        "bookingId",
        "bookingReference",
        "currency",
        "status",
        "paymentMethod",
        "paidAdditionalPaymentAmount",
        "payAdditionalPaymentAt",
        "RequiresAdditionalPaymentAmount",
        "failureReason"
})
public class ProcessAddiPaymentResponseDto {
    private Long id;
    private String paymentIntentId;
    private String transactionReference;

    private Long bookingId;
    private String bookingReference;

    private String currency;

    private Payment.PaymentStatus status;
    private Payment.PaymentMethod paymentMethod;


    private BigDecimal RequiresAdditionalPaymentAmount;
    private BigDecimal paidAdditionalPaymentAmount;
    private LocalDateTime payAdditionalPaymentAt;

    // If failed
    private String failureReason;
}
