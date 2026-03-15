package com.worldcup.hotelbooking.payment;

import com.worldcup.hotelbooking.booking.booking.Booking;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne()
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(unique = true, length = 100)
    private String transactionReference;

    @Column(unique = true, length = 100)
    private String paymentIntentId;

    // Refund fields
    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount;


    private LocalDateTime refundedAt;

    @Column(length = 500)
    private String refundReason;


    private BigDecimal RequiredAdditionalPaymentAmount;
    private BigDecimal paidAdditionalPaymentAmount;
    private LocalDateTime payAdditionalPaymentAt;


    // Payment timestamps
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private BigDecimal paidAmount;

    // Failure tracking
    @Column(length = 500)
    private String failureReason;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (paymentIntentId == null) {
            paymentIntentId = generatePaymentIntentId();
        }
    }

    private String generatePaymentIntentId() {
        return "pi_" + System.currentTimeMillis() + "_" +
                java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public enum PaymentStatus {
        PENDING,        // Payment intent created, awaiting payment
        PROCESSING,     // Payment being processed
        COMPLETED,      // Payment successful
        FAILED,         // Payment failed
        CANCELLED,      // Payment cancelled
        REFUNDED,       // Full refund issued
        PARTIALLY_REFUNDED ,// Partial refund issued
        OVER_REFUNDED,// Refund amount exceeds original payment when the manger cancelled the booking
        PARTIALLY_PAID// Additional payment required when the manager cancelled the booking and the guest need to pay the additional payment
    }

    public enum PaymentMethod {
        CREDIT_CARD,
        DEBIT_CARD,
        PAYPAL,
        BANK_TRANSFER,
        CASH
    }
}