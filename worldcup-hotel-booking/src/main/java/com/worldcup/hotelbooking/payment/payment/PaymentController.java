package com.worldcup.hotelbooking.payment.payment;

import com.worldcup.hotelbooking.booking.booking.BookingServiceImp;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomServiceImp;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class PaymentController {
    private final PaymentServiceImpl paymentService;
    private final BookingServiceImp bookingService;

    PaymentController(PaymentServiceImpl paymentService, BookingServiceImp bookingService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponseDto> processPayment(@Valid @RequestBody PaymentRequestDto paymentRequest, UriComponentsBuilder uriBuilder) {
        Payment response = paymentService.CreatePayment(PaymentMapper.toEntity(paymentRequest, bookingService.getBookingById(paymentRequest.getBookingId())));

        return ResponseEntity.created(uriBuilder.path("/payments/{id}").buildAndExpand(response.getId()).toUri())
                .body(PaymentMapper.toPaymentResponseDto(response));

    }
}
