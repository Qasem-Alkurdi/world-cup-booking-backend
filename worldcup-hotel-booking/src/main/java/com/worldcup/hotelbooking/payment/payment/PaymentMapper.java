package com.worldcup.hotelbooking.payment.payment;

import com.worldcup.hotelbooking.booking.booking.Booking;

public class PaymentMapper {

    public static Payment toEntity(PaymentRequestDto requestDto, Booking booking) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(requestDto.getPaymentMethod());

        return payment;}
     public static PaymentResponseDto toPaymentResponseDto(Payment payment) {
        PaymentResponseDto responseDto = new PaymentResponseDto();
        responseDto.setPaymentId(payment.getId());
        responseDto.setBookingReference(payment.getBooking().getBookingReference());
        responseDto.setAmount(payment.getAmount());
        responseDto.setCurrency(payment.getCurrency());
        responseDto.setPaymentMethod(payment.getPaymentMethod());
        responseDto.setStatus(payment.getStatus());
        responseDto.setTransactionReference(payment.getTransactionReference());
        responseDto.setPaidAt(payment.getPaidAt());
        return responseDto;
    }


}
