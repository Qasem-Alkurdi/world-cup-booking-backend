package com.worldcup.hotelbooking.common.seeder;

import com.worldcup.hotelbooking.payment.Payment;
import com.worldcup.hotelbooking.payment.PaymentRepository;
import com.worldcup.hotelbooking.reservation.booking.Booking;
import com.worldcup.hotelbooking.reservation.booking.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * PaymentSeeder - Creates payments for bookings with various statuses
 * <p>
 * Payment scenarios:
 * - COMPLETED: Successfully paid bookings
 * - PENDING: Awaiting payment
 * - FAILED: Failed payment attempts
 * - REFUNDED: Full refunds for cancellations
 * - PARTIALLY_REFUNDED: Partial refunds (price decreases or early cancellations)
 * - PARTIALLY_PAID: Additional payment required after booking modification
 */
@Component
@Order(7)
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class PaymentSeeder implements CommandLineRunner {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    private final Random random = new Random(67890); // Fixed seed

    @Override
    @Transactional
    public void run(String... args) {
        if (paymentRepository.count() > 0) {
            log.info("Payments already exist. Skipping payment seeder.");
            return;
        }

        List<Booking> bookings = bookingRepository.findAll();
        if (bookings.isEmpty()) {
            log.warn("No bookings found. Run BookingSeeder first.");
            return;
        }

        List<Payment> payments = new ArrayList<>();

        for (Booking booking : bookings) {
            Payment payment = createPaymentForBooking(booking);
            if (payment != null) {
                payments.add(payment);
            }
        }

        paymentRepository.saveAll(payments);
        log.info("Seeded {} payments for {} bookings.", payments.size(), bookings.size());
    }

    private Payment createPaymentForBooking(Booking booking) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setTotalAmount(booking.getTotalPrice());
        payment.setCurrency("USD");
        payment.setPaymentMethod(randomPaymentMethod());

        // Set payment status and details based on booking status
        switch (booking.getStatus()) {
            case CONFIRMED -> createConfirmedPayment(payment, booking);
            case PENDING -> createPendingPayment(payment);
            case CHECKED_IN -> createCheckedInPayment(payment, booking);
            case CHECKED_OUT -> createCheckedOutPayment(payment, booking);
            case CANCELLED -> createCancelledPayment(payment, booking);
        }

        return payment;
    }

    private void createConfirmedPayment(Payment payment, Booking booking) {
        if (booking.isAdditionalPaymentRequired()) {
            // Booking modified with price increase - additional payment required
            payment.setStatus(Payment.PaymentStatus.PARTIALLY_PAID);

            // Calculate original payment (80% of total as example)
            BigDecimal originalAmount = payment.getTotalAmount()
                    .multiply(BigDecimal.valueOf(0.80))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal additionalRequired = payment.getTotalAmount()
                    .subtract(originalAmount);

            payment.setPaidAmount(originalAmount);
            payment.setRequiredAdditionalPaymentAmount(additionalRequired);
            payment.setPaidAdditionalPaymentAmount(BigDecimal.ZERO);
            payment.setPaidAt(LocalDateTime.now().minusDays(random.nextInt(15) + 5));
            payment.setTransactionReference(generateTransactionRef());

        } else {
            // Normal completed payment
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaidAmount(payment.getTotalAmount());
            payment.setRequiredAdditionalPaymentAmount(BigDecimal.ZERO);
            payment.setPaidAdditionalPaymentAmount(BigDecimal.ZERO);
            payment.setPaidAt(LocalDateTime.now().minusDays(random.nextInt(30) + 1));
            payment.setTransactionReference(generateTransactionRef());
        }
    }

    private void createPendingPayment(Payment payment) {
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setPaidAmount(BigDecimal.ZERO);
        payment.setRequiredAdditionalPaymentAmount(BigDecimal.ZERO);
        payment.setPaidAdditionalPaymentAmount(BigDecimal.ZERO);
        // No paidAt or transaction reference yet
    }

    private void createCheckedInPayment(Payment payment, Booking booking) {
        // Already checked in, so payment must be completed
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAmount(payment.getTotalAmount());
        payment.setRequiredAdditionalPaymentAmount(BigDecimal.ZERO);
        payment.setPaidAdditionalPaymentAmount(BigDecimal.ZERO);
        payment.setPaidAt(booking.getConfirmedAt() != null
                ? booking.getConfirmedAt()
                : LocalDateTime.now().minusDays(5));
        payment.setTransactionReference(generateTransactionRef());
    }

    private void createCheckedOutPayment(Payment payment, Booking booking) {
        // Completed stay - payment was successful
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAmount(payment.getTotalAmount());
        payment.setRequiredAdditionalPaymentAmount(BigDecimal.ZERO);
        payment.setPaidAdditionalPaymentAmount(BigDecimal.ZERO);
        payment.setPaidAt(booking.getConfirmedAt() != null
                ? booking.getConfirmedAt()
                : LocalDateTime.now().minusDays(10));
        payment.setTransactionReference(generateTransactionRef());
    }

    private void createCancelledPayment(Payment payment, Booking booking) {
        // Cancelled booking - determine refund amount based on timing
        LocalDateTime cancelledAt = booking.getCancelledAt() != null
                ? booking.getCancelledAt()
                : LocalDateTime.now().minusDays(1);

        long daysBeforeCheckIn = java.time.temporal.ChronoUnit.DAYS.between(
                cancelledAt.toLocalDate(),
                booking.getCheckInDate()
        );

        // Calculate refund percentage based on cancellation policy
        BigDecimal refundPercentage;
        String refundReason;

        if (daysBeforeCheckIn >= 30) {
            refundPercentage = BigDecimal.valueOf(1.00); // 100% refund
            refundReason = "Full refund - cancelled 30+ days before check-in";
        } else if (daysBeforeCheckIn >= 14) {
            refundPercentage = BigDecimal.valueOf(0.75); // 75% refund
            refundReason = "75% refund - cancelled 14-29 days before check-in";
        } else if (daysBeforeCheckIn >= 7) {
            refundPercentage = BigDecimal.valueOf(0.50); // 50% refund
            refundReason = "50% refund - cancelled 7-13 days before check-in";
        } else if (daysBeforeCheckIn >= 3) {
            refundPercentage = BigDecimal.valueOf(0.25); // 25% refund
            refundReason = "25% refund - cancelled 3-6 days before check-in";
        } else {
            refundPercentage = BigDecimal.ZERO; // No refund
            refundReason = "No refund - cancelled less than 3 days before check-in";
        }

        BigDecimal refundAmount = payment.getTotalAmount()
                .multiply(refundPercentage)
                .setScale(2, RoundingMode.HALF_UP);

        // Set payment details
        payment.setPaidAmount(payment.getTotalAmount());
        payment.setPaidAt(LocalDateTime.now().minusDays(random.nextInt(40) + 10));
        payment.setTransactionReference(generateTransactionRef());
        payment.setRequiredAdditionalPaymentAmount(BigDecimal.ZERO);
        payment.setPaidAdditionalPaymentAmount(BigDecimal.ZERO);

        if (refundAmount.compareTo(BigDecimal.ZERO) == 0) {
            // No refund - payment completed but not refunded
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
        } else if (refundAmount.compareTo(payment.getTotalAmount()) == 0) {
            // Full refund
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            payment.setRefundAmount(refundAmount);
            payment.setRefundedAt(cancelledAt);
            payment.setRefundReason(refundReason);
        } else {
            // Partial refund
            payment.setStatus(Payment.PaymentStatus.PARTIALLY_REFUNDED);
            payment.setRefundAmount(refundAmount);
            payment.setRefundedAt(cancelledAt);
            payment.setRefundReason(refundReason);
        }
    }

    private Payment.PaymentMethod randomPaymentMethod() {
        Payment.PaymentMethod[] methods = Payment.PaymentMethod.values();
        // Weight towards CREDIT_CARD (70%), DEBIT_CARD (20%), others (10%)
        int rand = random.nextInt(100);
        if (rand < 70) {
            return Payment.PaymentMethod.CREDIT_CARD;
        } else if (rand < 90) {
            return Payment.PaymentMethod.DEBIT_CARD;
        } else {
            return methods[random.nextInt(methods.length)];
        }
    }

    private String generateTransactionRef() {
        return "txn_" + System.currentTimeMillis() + "_" +
                randomAlphaNumeric(8).toUpperCase();
    }

    private String randomAlphaNumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}