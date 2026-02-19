package com.worldcup.hotelbooking.booking.cancellation;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CancellationPolicyResponse {
    private boolean canCancel;
    private BigDecimal refundAmount;
    private BigDecimal cancellationFee;
    private int refundPercentage;
    private String policyMessage;
    private long daysUntilCheckIn;
    private String summary;
}
