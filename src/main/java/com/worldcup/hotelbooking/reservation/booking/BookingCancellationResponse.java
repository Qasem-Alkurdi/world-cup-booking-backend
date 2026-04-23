package com.worldcup.hotelbooking.reservation.booking;

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

    // ── Manager-cancellation bonus fields (null / zero for guest cancellations) ──
    /**
     * Extra compensation on top of the base refund when the hotel cancels.
     */
    private BigDecimal bonusAmount;
    /**
     * Human-readable bonus tier description (null for guest cancellations).
     */
    private String bonusTierDescription;
    /**
     * Total payout to the guest: refundAmount + bonusAmount.
     */
    private BigDecimal totalPayout;
    /**
     * Who cancelled: guest username OR manager username.
     */
    private String cancelledBy;
}