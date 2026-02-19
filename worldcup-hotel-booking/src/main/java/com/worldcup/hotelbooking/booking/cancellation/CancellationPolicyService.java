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
public class CancellationPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(CancellationPolicyService.class);

    /**
     * Check if booking can be cancelled and calculate refund
     *
     * @throws CancellationNotAllowedException if booking cannot be cancelled
     */
    public CancellationResult calculateCancellation(Booking booking) {

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

        return new CancellationResult(
                true,  // Can cancel
                refundAmount,
                cancellationFee,
                refundPercentage,
                policyApplied,
                daysUntilCheckIn
        );
    }

    /**
     * Get cancellation policy info without actually cancelling
     * Shows user what they would get if they cancel now
     */
    public CancellationResult previewCancellation(Booking booking) {
        try {
            return calculateCancellation(booking);
        } catch (CancellationNotAllowedException e) {
            return new CancellationResult(
                    false,  // Cannot cancel
                    BigDecimal.ZERO,
                    booking.getTotalPrice(),
                    0,
                    e.getMessage(),
                    0
            );
        }
    }
}