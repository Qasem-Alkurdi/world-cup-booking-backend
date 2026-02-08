
package com.worldcup.hotelbooking.payment.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.worldcup.hotelbooking.booking.booking.Booking;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Data
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payment_booking", columnList = "booking_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_transaction", columnList = "transaction_reference")
    }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;



    @Column(name = "transaction_reference", unique = true, length = 100)
    private String transactionReference;

    @NotNull
    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency = "USD";

    @NotNull
    @Column(name = "payment_method", length = 30, nullable = false)
    private String paymentMethod;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Lob
    @Column(name = "refund_reason")
    private String refundReason;

    @Lob//to store big data
    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp//it is creat the time when the record is created
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    public Payment() {}

    public Payment(Booking booking, String transactionReference, BigDecimal amount,
                   String paymentMethod, String status, BigDecimal refundAmount, String refundReason,
                   String failureReason, LocalDateTime paidAt, LocalDateTime refundedAt) {
        this.booking = booking;
        this.transactionReference = transactionReference;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.refundAmount = refundAmount;
        this.refundReason = refundReason;
        this.failureReason = failureReason;
        this.paidAt = paidAt;
        this.refundedAt = refundedAt;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

}