package com.worldcup.hotelbooking.booking.cancellation;

import com.worldcup.hotelbooking.booking.booking.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Cancellation Policy Service
 * <p>
 * World Cup 2026 Hotel Booking Cancellation Rules:
 * - 30+ days before check-in: Full refund (100%)
 * - 14-29 days before: 75% refund
 * - 7-13 days before: 50% refund
 * - 3-6 days before: 25% refund
 * - Less than 3 days: No refund (0%)
 * - After check-in: Cannot cancel
 * - Already cancelled: Cannot cancel again
 */
@Service
public class CancellationPolicyServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(CancellationPolicyServiceImpl.class);

    /**
     * Check if booking can be cancelled and calculate refund
     *
     * @throws CancellationNotAllowedException if booking cannot be cancelled
     */
    public CancellationResponse calculateCancellation(Booking booking) {

        // Rule 1: Cannot cancel if already cancelled
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new CancellationNotAllowedException(
                    "Booking is already cancelled"
            );
        }

        // Rule 2: Cannot cancel if already checked in or checked out
        if (booking.getStatus() == Booking.BookingStatus.CHECKED_IN) {
            throw new CancellationNotAllowedException(
                    "Cannot cancel after check-in. Please contact hotel reception."
            );
        }

        if (booking.getStatus() == Booking.BookingStatus.CHECKED_OUT) {
            throw new CancellationNotAllowedException(
                    "Cannot cancel after check-out"
            );
        }

        // Rule 3: Cannot cancel if check-in date has passed
        if (booking.getCheckInDate().isBefore(LocalDate.now())) {
            throw new CancellationNotAllowedException(
                    "Cannot cancel after check-in date has passed"
            );
        }

        // Calculate days until check-in
        long daysUntilCheckIn = ChronoUnit.DAYS.between(
                LocalDate.now(),
                booking.getCheckInDate()
        );

        logger.info("Cancellation check: Booking {}, {} days until check-in",
                booking.getBookingReference(), daysUntilCheckIn);

        // Calculate refund based on cancellation policy
        BigDecimal totalPaid = booking.getTotalPrice();
        BigDecimal refundAmount;
        BigDecimal cancellationFee;
        int refundPercentage;
        String policyApplied;

        if (daysUntilCheckIn >= 30) {
            // 30+ days: Full refund
            refundPercentage = 100;
            refundAmount = totalPaid;
            cancellationFee = BigDecimal.ZERO;
            policyApplied = "Full refund - 30+ days notice";

        } else if (daysUntilCheckIn >= 14) {
            // 14-29 days: 75% refund
            refundPercentage = 75;
            refundAmount = totalPaid.multiply(BigDecimal.valueOf(0.75));
            cancellationFee = totalPaid.subtract(refundAmount);
            policyApplied = "75% refund - 14-29 days notice";

        } else if (daysUntilCheckIn >= 7) {
            // 7-13 days: 50% refund
            refundPercentage = 50;
            refundAmount = totalPaid.multiply(BigDecimal.valueOf(0.50));
            cancellationFee = totalPaid.subtract(refundAmount);
            policyApplied = "50% refund - 7-13 days notice";

        } else if (daysUntilCheckIn >= 3) {
            // 3-6 days: 25% refund
            refundPercentage = 25;
            refundAmount = totalPaid.multiply(BigDecimal.valueOf(0.25));
            cancellationFee = totalPaid.subtract(refundAmount);
            policyApplied = "25% refund - 3-6 days notice";

        } else {
            // Less than 3 days: No refund
            refundPercentage = 0;
            refundAmount = BigDecimal.ZERO;
            cancellationFee = totalPaid;
            policyApplied = "No refund - Less than 3 days notice";
        }

        logger.info("Cancellation policy applied: {} - Refund: ${} ({}%)",
                policyApplied, refundAmount, refundPercentage);

        return new CancellationResponse(
                true,  // Can cancel
                refundAmount,
                cancellationFee,
                refundPercentage,
                policyApplied,
                daysUntilCheckIn,
                null,  // bonusAmount — not applicable for guest cancellations
                null   // bonusTierDescription — not applicable for guest cancellations
        );
    }

    /**
     * Calculate refund + compensation bonus when a HOTEL MANAGER cancels a guest's booking.
     *
     * Rules:
     *  - Guest always receives 100% base refund (hotel's fault, never penalise the guest).
     *  - On top of the base refund, a compensation bonus is added — the closer to check-in,
     *    the higher the bonus, because the disruption is greater:
     *
     *      30+  days  →  +10%  (low inconvenience, courtesy bonus)
     *      14-29 days  →  +25%
     *       7-13 days  →  +35%
     *        3-6 days  →  +40%
     *        < 3 days  →  +50%  (maximum disruption)
     *
     * Example: booking $200, cancelled 2 days before check-in
     *   base refund  = $200  (100%)
     *   bonus        = $100  (50% of $200)
     *   total payout = $300
     */
    public CancellationResponse calculateManagerCancellation(Booking booking) {

        // Same status guards as guest cancellation
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new CancellationNotAllowedException("Booking is already cancelled.");
        }
        if (booking.getStatus() == Booking.BookingStatus.CHECKED_IN) {
            throw new CancellationNotAllowedException(
                    "Guest has already checked in. Contact hotel reception to resolve.");
        }
        if (booking.getStatus() == Booking.BookingStatus.CHECKED_OUT) {
            throw new CancellationNotAllowedException("Booking is already completed (checked-out).");
        }

        long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), booking.getCheckInDate());

        BigDecimal baseRefund = booking.getTotalPrice();   // always 100%
        int bonusPercentage;
        String bonusTierDescription;

        // Mirror-opposite of guest penalty tiers — worse disruption = higher bonus
        if (daysUntilCheckIn >= 30) {
            bonusPercentage      = 10;
            bonusTierDescription = "10% courtesy bonus — 30+ days notice";
        } else if (daysUntilCheckIn >= 14) {
            bonusPercentage      = 25;
            bonusTierDescription = "25% compensation bonus — 14-29 days notice";
        } else if (daysUntilCheckIn >= 7) {
            bonusPercentage      = 35;
            bonusTierDescription = "35% compensation bonus — 7-13 days notice";
        } else if (daysUntilCheckIn >= 3) {
            bonusPercentage      = 40;
            bonusTierDescription = "40% compensation bonus — 3-6 days notice";
        } else {
            bonusPercentage      = 50;
            bonusTierDescription = "50% compensation bonus — less than 3 days notice";
        }

        BigDecimal bonusAmount = baseRefund
                .multiply(BigDecimal.valueOf(bonusPercentage))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

        logger.info("Manager cancellation — days until check-in: {}, bonus: {}% (${}) — total payout: ${}",
                daysUntilCheckIn, bonusPercentage, bonusAmount, baseRefund.add(bonusAmount));

        return CancellationResponse.builder()
                .canCancel(true)
                .refundAmount(baseRefund)
                .cancellationFee(BigDecimal.ZERO)       // guest pays no fee
                .refundPercentage(100)
                .policyMessage("Hotel-initiated cancellation — full refund + " + bonusTierDescription)
                .daysUntilCheckIn(daysUntilCheckIn)
                .bonusAmount(bonusAmount)
                .bonusTierDescription(bonusTierDescription)
                .build();
    }

    /**
     * Get cancellation policy info without actually cancelling
     * Shows user what they would get if they cancel now
     */
    public CancellationResponse previewCancellation(Booking booking) {
        try {
            return calculateCancellation(booking);
        } catch (CancellationNotAllowedException e) {
            return new CancellationResponse(
                    false,  // Cannot cancel
                    BigDecimal.ZERO,
                    booking.getTotalPrice(),
                    0,
                    e.getMessage(),
                    0,
                    null,  // bonusAmount
                    null   // bonusTierDescription
            );
        }
    }

    public CancellationResponse previewManagerCancellation(Booking booking) {
        try {
            return calculateManagerCancellation(booking);
        } catch (CancellationNotAllowedException e) {
            return new CancellationResponse(
                    false,  // Cannot cancel
                    BigDecimal.ZERO,
                    booking.getTotalPrice(),
                    0,
                    e.getMessage(),
                    0,
                    null,  // bonusAmount
                    null   // bonusTierDescription
            );
        }
    }
}