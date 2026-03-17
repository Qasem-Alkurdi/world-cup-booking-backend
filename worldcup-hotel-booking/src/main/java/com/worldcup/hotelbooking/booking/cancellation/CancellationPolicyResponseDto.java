package com.worldcup.hotelbooking.booking.cancellation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Builder
public class CancellationPolicyResponseDto {
    private boolean canCancel;
    private BigDecimal refundAmount;
    private BigDecimal cancellationFee;
    private int refundPercentage;
    private String policyMessage;
    private long daysUntilCheckIn;
    private String summary;
}
