package com.worldcup.hotelbooking.reservation.cancellation;

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

    // ── Manager-cancellation bonus fields (zero / null for guest cancellations) ──
    /**
     * Extra compensation on top of the base refund when the hotel cancels.
     */
    private BigDecimal bonusAmount;
    /**
     * Human-readable bonus tier description (null for guest cancellations).
     */
    private String bonusTierDescription;
    /**
     * Total payout: refundAmount + bonusAmount.
     */
    private BigDecimal totalPayout;
}