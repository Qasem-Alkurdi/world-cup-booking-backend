package com.worldcup.hotelbooking.booking.cancellation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Result of cancellation calculation
 */
@Getter
@AllArgsConstructor
@Builder//this anotation allows us to easily create instances of this class using a builder pattern, which is especially useful when there are multiple fields to set.
public class CancellationResponseDto {

    private boolean canCancel;
    private BigDecimal refundAmount;
    private BigDecimal cancellationFee;
    private int refundPercentage;
    private String policyMessage;
    private long daysUntilCheckIn;

    public boolean hasRefund() {
        return refundAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isFullRefund() {
        return refundPercentage == 100;
    }

    public String getSummary() {
        if (!canCancel) {
            return policyMessage;
        }

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
