package com.worldcup.hotelbooking.payment;

import com.worldcup.hotelbooking.booking.booking.Booking;

import java.math.BigDecimal;

public class PaymentMapper {
    public static PaymentIntentResponseDto toPaymentIntentDto(Payment payment) {
        return new PaymentIntentResponseDto(
                payment.getPaymentIntentId(),
                payment.getId(),
                payment.getBooking().getId(),
                payment.getBooking().getBookingReference(),
                payment.getTotalAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getCreatedAt());
    }

    public static Payment toEntity(PaymentIntentRequestDto dto, Booking booking) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(dto.getPaymentMethod());

        return payment;
    }

   public static  ProcessPaymentResponseDto toProcessPaymentResponseDto(Payment payment) {
        return new ProcessPaymentResponseDto(payment.getId(),
                payment.getPaymentIntentId(),
                payment.getTransactionReference(),
                payment.getBooking().getId(),
                payment.getBooking().getBookingReference(),
                payment.getTotalAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getCreatedAt(),
                payment.getPaidAt(),
                payment.getFailureReason()
        );
    }

    public static PaymentResponseDto toPaymentResponseDto(Payment payment) {
        return new PaymentResponseDto(payment.getId(),
                payment.getPaymentIntentId(),
                payment.getTransactionReference(),
                payment.getBooking().getId(),
                payment.getBooking().getBookingReference(),
                payment.getTotalAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getCreatedAt(),
                payment.getPaidAt(),
                payment.getPaidAmount(),
                payment.getFailureReason(),
                payment.getRefundAmount(),
                payment.getRefundedAt(),
                payment.getRefundReason(),
                payment.getRequiredAdditionalPaymentAmount(),
                payment.getPaidAdditionalPaymentAmount(),
                payment.getPayAdditionalPaymentAt()
        );
    }

    public static RefundResponseDto toRefundResponseDto(Payment payment) {
        return new RefundResponseDto(payment.getId(),
                payment.getTransactionReference(),
                payment.getTotalAmount(),
                payment.getRefundAmount(),
                payment.getTotalAmount().subtract(payment.getRefundAmount()),
                payment.getStatus(),
                payment.getRefundedAt(),
                payment.getRefundReason(),
                true,
                "Refund processed successfully"
        );
    }

    public static ProcessAddiPaymentResponseDto processAddiPaymentResponseDto(Payment payment) {
        return new ProcessAddiPaymentResponseDto(payment.getId(),
                payment.getPaymentIntentId(),
                payment.getTransactionReference(),
                payment.getBooking().getId(),
                payment.getBooking().getBookingReference(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getRequiredAdditionalPaymentAmount(),
                payment.getPaidAdditionalPaymentAmount(),
                payment.getPayAdditionalPaymentAt(),
                payment.getFailureReason()
        );
    }
}
