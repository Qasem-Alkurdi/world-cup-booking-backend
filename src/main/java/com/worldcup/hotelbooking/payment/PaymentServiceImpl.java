package com.worldcup.hotelbooking.payment;

import com.worldcup.hotelbooking.reservation.booking.Booking;
import com.worldcup.hotelbooking.reservation.booking.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PaymentServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;


    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    // ========================================
    // CREATE PAYMENT INTENT
    // ========================================

    /**
     * Create a payment intent for a booking
     * This is step 1: Create the payment record with PENDING status
     */
    public Payment createPaymentIntent(Payment payment) {
        logger.info("Creating payment intent for booking: {}", payment.getBooking().getId());

        // 2. Validate booking can be paid
        validateBookingForPayment(payment.getBooking());

        // 3. Check if payment already exists
        if (paymentRepository.existsByBookingId(payment.getBooking().getId())) {
            throw new PaymentException("Payment already exists for this booking");
        }


        payment.setTotalAmount(payment.getBooking().getTotalPrice());
        payment.setCurrency("USD");
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setRequiredAdditionalPaymentAmount(BigDecimal.valueOf(0));
        payment.setPaidAdditionalPaymentAmount(BigDecimal.valueOf(0));
        payment.setPaidAmount(BigDecimal.valueOf(0));

        Payment savedPayment = paymentRepository.save(payment);

        logger.info("Payment intent created: {} for booking: {}",
                savedPayment.getPaymentIntentId(), payment.getBooking().getBookingReference());

        // 5. Build response
        return savedPayment;
    }

    // ========================================
    // PROCESS PAYMENT (MOCK)
    // ========================================

    /**
     * Process payment - MOCK implementation
     * Simulates payment gateway processing
     */
    @Caching(evict = {
            @CacheEvict(value = "bookingById", key = "#result.booking.id"),
            @CacheEvict(value = "bookingByReference", allEntries = true),
            @CacheEvict(value = "hotelUpcoming", allEntries = true),
            @CacheEvict(value = "guestHistory", allEntries = true)
    })
    @Transactional
    public Payment processPayment(ProcessPaymentRequestDto request) {// in real app, we would not have the simulateSuccess field, it's just for testing different scenarios , because any one can send that process done from teh frontend and we want to make sure that we can test both success and failure scenarios and the right way is to sure from stripe or any other payment gateway that the payment is done and we can not trust the frontend to send us that information because it can be easily manipulated, so we need to have a way to simulate both scenarios for testing purposes
        logger.info("Processing payment for intent: {}", request.getPaymentIntentId());//I pass Dto becuase I need the simulateSuccess field to test both scenarios, in the real world we would only pass the payment

    
        // 1. Find payment by intent ID
        Payment payment = paymentRepository.findByPaymentIntentId(request.getPaymentIntentId())
                .orElseThrow(() -> new PaymentException("Payment intent not found"));

        // 2. Validate payment can be processed
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new PaymentException("Payment already processed or cancelled");
        }

        // 3. Update to PROCESSING
        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // 4. MOCK PAYMENT PROCESSING
        boolean paymentSuccess = mockPaymentProcessing(request);

        if (paymentSuccess) {
            // Success: Update payment
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            payment.setPaidAmount(payment.getTotalAmount());
            payment.setTransactionReference(generateTransactionReference());
            payment.setFailureReason(" "); // Clear any previous failure reason

            // Update booking status to CONFIRMED
            Booking booking = payment.getBooking();
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            booking.setConfirmedAt(java.time.LocalDateTime.now());
            bookingRepository.save(booking);

            logger.info("Payment successful: {} for booking: {}",
                    payment.getTransactionReference(), booking.getBookingReference());
        } else {
            // Failure: Update payment
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(request.getSimulateSuccess() != null && !request.getSimulateSuccess()
                    ? "Mock payment failure - simulated"
                    : "Insufficient funds");

            logger.warn("Payment failed for intent: {}", request.getPaymentIntentId());
        }

        Payment savedPayment = paymentRepository.save(payment);


        return savedPayment;
    }

    @Transactional
    // Additional payment processing for extra services or late check-out
    @Caching(evict = {
            @CacheEvict(value = "bookingById", key = "#result.booking.id"),
            @CacheEvict(value = "bookingByReference", allEntries = true),
            @CacheEvict(value = "hotelUpcoming", allEntries = true),
            @CacheEvict(value = "guestHistory", allEntries = true)
    })
    public Payment processAdditionalPayment(ProcessPaymentRequestDto request) {
        logger.info("Processing payment for intent: {}", request.getPaymentIntentId());

        // 1. Find payment by intent ID
        Payment payment = paymentRepository.findByPaymentIntentId(request.getPaymentIntentId())
                .orElseThrow(() -> new PaymentException("Payment intent not found"));

        // 2. Validate payment can be processed
        if (payment.getStatus() != Payment.PaymentStatus.PARTIALLY_PAID && payment.getStatus() != Payment.PaymentStatus.PENDING && payment.getStatus() != Payment.PaymentStatus.FAILED) {
            throw new PaymentException("The payment should be in PENDING or PARTIALLY_PAID status to be processed");
        }

        // 3. Update to PROCESSING
        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // 4. MOCK PAYMENT PROCESSING
        boolean paymentSuccess = mockPaymentProcessing(request);

        if (paymentSuccess) {
            // Success: Update payment
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPayAdditionalPaymentAt(LocalDateTime.now());
            payment.setPaidAmount(payment.getPaidAmount().add(payment.getRequiredAdditionalPaymentAmount()));
            payment.setTransactionReference(generateTransactionReference());
            payment.setFailureReason(" "); // Clear any previous failure reason
            payment.setPaidAdditionalPaymentAmount(payment.getRequiredAdditionalPaymentAmount());
            payment.setRequiredAdditionalPaymentAmount(BigDecimal.valueOf(0));// Clear additional payment amount


            // Update booking status to CONFIRMED
            Booking booking = payment.getBooking();
            booking.setAdditionalPaymentRequired(false);

            // ── Clear the update deadline ─────────────────────────────────────
            booking.setUpdatePaymentDeadline(null);
            bookingRepository.save(booking);

            // ── Delete the inactive snapshot copy — payment was completed ─────
            // We look up by bookingReference (the business key), not by id.
            bookingRepository.findInactiveSnapshotByOriginalReference(booking.getBookingReference())
                    .ifPresent(bookingRepository::delete);

            logger.info("✅ Snapshot copy deleted for booking {} — additional payment completed",
                    booking.getBookingReference());


            logger.info("Payment successful: {} for booking: {}",
                    payment.getTransactionReference(), booking.getBookingReference());
        } else {
            // Failure: Update payment
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(request.getSimulateSuccess() != null && !request.getSimulateSuccess()
                    ? "Mock payment failure - simulated"
                    : "Insufficient funds");

            logger.warn("Payment failed for intent: {}", request.getPaymentIntentId());
        }

        Payment savedPayment = paymentRepository.save(payment);


        return savedPayment;
    }

    // ========================================
    // REFUND PAYMENT
    // ========================================

    /**
     * Process refund for a payment
     * Called when booking is cancelled
     */
    // ...existing code...
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookingById", key = "#result.booking.id"),
            @CacheEvict(value = "bookingByReference", allEntries = true),
            @CacheEvict(value = "hotelUpcoming", allEntries = true),
            @CacheEvict(value = "guestHistory", allEntries = true)
    })
    public Payment refundPayment(RefundRequestDto request) {

        logger.info("Processing refund for payment: {}", request.getPaymentId());

        // 1️⃣ Find payment
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new PaymentException("Payment not found"));

        // 2️⃣ Validate payment status
        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED &&
                payment.getStatus() != Payment.PaymentStatus.PARTIALLY_REFUNDED) {

            throw new PaymentException("Cannot refund payment with status: " + payment.getStatus());
        }

        // 3️⃣ Determine refund amount
        BigDecimal refundAmount = request.getRefundAmount() != null
                ? request.getRefundAmount()
                : payment.getPaidAmount();

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Refund amount must be greater than zero");
        }

        // 4️⃣ Current refund tracking
        BigDecimal alreadyRefunded = payment.getRefundAmount() != null
                ? payment.getRefundAmount()
                : BigDecimal.ZERO;

        BigDecimal paidAmount = payment.getPaidAmount() != null
                ? payment.getPaidAmount()
                : BigDecimal.ZERO;

        // 5️⃣ Calculate total refund after this transaction
        BigDecimal totalRefunded = alreadyRefunded.add(refundAmount);

        // 6️⃣ Process refund (mock gateway)
        boolean refundSuccess = mockRefundProcessing(payment, refundAmount);

        if (!refundSuccess) {
            throw new PaymentException("Refund processing failed");
        }

        // 8️⃣ Update refund totals
        payment.setRefundAmount(totalRefunded);
        payment.setRefundReason(request.getReason());
        payment.setRefundedAt(LocalDateTime.now());

        // 9️⃣ ⭐ CORRECT STATUS LOGIC (compare against PAID amount, not total amount!)
        if (totalRefunded.compareTo(paidAmount) == 0) {
            // Refunded EVERYTHING that was paid
            payment.setStatus(Payment.PaymentStatus.REFUNDED);

        } else if (totalRefunded.compareTo(paidAmount) < 0) {
            // Refunded SOME of what was paid
            payment.setStatus(Payment.PaymentStatus.PARTIALLY_REFUNDED);

        } else {
            // Refund exceeds what was paid — manager cancellation bonus case
            payment.setStatus(Payment.PaymentStatus.OVER_REFUNDED);
        }

        // 🔟 Save payment
        Payment savedPayment = paymentRepository.save(payment);

        logger.info("✅ Refund successful: $%.2f refunded. Total refunded: $%.2f / $%.2f paid",
                refundAmount, totalRefunded, paidAmount);

        return savedPayment;
    }

    // ========================================
    // QUERY METHODS
    // ========================================

    @Transactional
    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentException("Payment not found"));
    }

    @Transactional
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new PaymentException("Payment not found for booking"));

    }

    @Transactional
    public Page<Payment> getUserPayments(Long userId, Pageable pageable) {
        return paymentRepository.findByBooking_AppUser_Id(userId, pageable);
    }

    @Transactional
    public Page<Payment> getHotelPayments(Long hotelId, Pageable pageable) {
        return paymentRepository.findByBooking_Hotel_Id(hotelId, pageable);

    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private void validateBookingForPayment(Booking booking) {
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new PaymentException("Cannot create payment for cancelled booking");
        }
        if (booking.getStatus() == Booking.BookingStatus.CHECKED_OUT) {
            throw new PaymentException("Cannot create payment for checked-out booking");
        }
        if (booking.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Invalid booking amount");
        }
    }

    /**
     * MOCK: Simulate payment gateway processing
     * In real app, this would call Stripe/PayPal API
     */
    private boolean mockPaymentProcessing(ProcessPaymentRequestDto request) {
        // Simulate processing delay
        try {
            Thread.sleep(1000);  // 1 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Check if explicit failure requested
        return request.getSimulateSuccess() == null || request.getSimulateSuccess();

        // Random failure (10% chance)
    }

    /**
     * MOCK: Simulate refund processing
     * In real app, this would call Stripe/PayPal refund API
     */
    private boolean mockRefundProcessing(Payment payment, BigDecimal amount) {
        // Simulate processing delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Mock always succeeds
        logger.info("MOCK REFUND: ${} to transaction {}",
                amount, payment.getTransactionReference());
        return true;
    }

    private String generateTransactionReference() {
        return "txn_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }


}