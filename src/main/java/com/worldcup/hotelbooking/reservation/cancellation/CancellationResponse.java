package com.worldcup.hotelbooking.reservation.cancellation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Result of cancellation calculation
 */
@Getter
@AllArgsConstructor
@Builder
//this anotation allows us to easily create instances of this class using a builder pattern, which is especially useful when there are multiple fields to set.
public class CancellationResponse {

    private boolean canCancel;
    private BigDecimal refundAmount;
    private BigDecimal cancellationFee;
    private int refundPercentage;
    private String policyMessage;
    private long daysUntilCheckIn;

    // ── Manager-cancellation bonus fields (zero / null for guest cancellations) ──
    /**
     * Extra compensation added on top of the base refund when the hotel cancels.
     */
    private BigDecimal bonusAmount;
    /**
     * Human-readable description of the bonus tier applied (null for guest cancellations).
     */
    private String bonusTierDescription;

    public boolean hasRefund() {
        return refundAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isFullRefund() {
        return refundPercentage == 100;
    }

    /**
     * Total payout to the guest: refundAmount + bonusAmount (bonus is 0 for guest cancellations).
     */
    public BigDecimal getTotalPayout() {
        BigDecimal bonus = (bonusAmount != null) ? bonusAmount : BigDecimal.ZERO;
        return refundAmount.add(bonus);
    }

    public String getSummary() {
        if (!canCancel) {
            return policyMessage;
        }

        if (bonusAmount != null && bonusAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Manager-initiated cancellation — show bonus breakdown
            return String.format(
                    "Cancellation Policy: %s\n" +
                            "Days until check-in: %d\n" +
                            "Base refund: $%.2f (100%%)\n" +
                            "Compensation bonus: $%.2f (%s)\n" +
                            "Cancellation fee charged to guest: $%.2f",
                    policyMessage,
                    daysUntilCheckIn,
                    refundAmount,
                    bonusAmount,
                    bonusTierDescription,
                    cancellationFee
            );
        }

        // Guest-initiated cancellation — original format
        return String.format(
                "Cancellation Policy: %s\n" +
                        "Days until check-in: %d\n" +
                        "Refund: $%.2f (%d%%)\n" +
                        "Cancellation fee: $%.2f",
                policyMessage,
                daysUntilCheckIn,
                refundAmount,
                refundPercentage,
                cancellationFee
        );
    }
}