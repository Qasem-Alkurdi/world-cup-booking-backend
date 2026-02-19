package com.worldcup.hotelbooking.booking.booking;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class BookingCancellationResponse {
    private BookingResponseDto booking;
    private BigDecimal refundAmount;
    private BigDecimal cancellationFee;
    private int refundPercentage;
    private String policyApplied;
}
