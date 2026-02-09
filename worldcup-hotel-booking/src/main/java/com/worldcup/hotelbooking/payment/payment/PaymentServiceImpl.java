package com.worldcup.hotelbooking.payment.payment;

import com.worldcup.hotelbooking.booking.booking.Booking;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {
    private PaymentRepository paymentRepository;
    PaymentServiceImpl (PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Payment CreatePayment(Payment payment) {

        payment.setTransactionReference(generateTransactionRef());

        if(payment.getBooking().getStatus()!= Booking.BookingStatus.PENDING)
            throw new IllegalStateException("Payment can only be made for pending bookings.");
        payment.setAmount(payment.getBooking().getTotalPrice());
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.getBooking().setStatus(Booking.BookingStatus.CONFIRMED);
        payment.getBooking().setConfirmedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        return payment;
    }

    private String generateTransactionRef() {
        return "PAY-" + UUID.randomUUID().toString().substring(0,8).toUpperCase();
    }

}
