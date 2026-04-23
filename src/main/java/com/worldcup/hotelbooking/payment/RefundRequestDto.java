package com.worldcup.hotelbooking.payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDto {

    @NotNull(message = "Payment ID is required")
    private Long paymentId;

    private BigDecimal refundAmount;  // If null, full refund

    @NotNull(message = "Refund reason is required")
    private String reason;
}