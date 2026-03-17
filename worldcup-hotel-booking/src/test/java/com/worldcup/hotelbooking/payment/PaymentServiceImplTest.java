package com.worldcup.hotelbooking.payment;

import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.booking.booking.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Booking booking;
    private Payment payment;

    @BeforeEach
    void setUp() {
        booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("WC2026-BOOK-001");
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setTotalPrice(BigDecimal.valueOf(500));

        payment = new Payment();
        payment.setId(10L);
        payment.setBooking(booking);
        payment.setPaymentIntentId("pi_test_123");
        payment.setPaymentMethod(Payment.PaymentMethod.CREDIT_CARD);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setTotalAmount(BigDecimal.valueOf(500));
        payment.setPaidAmount(BigDecimal.ZERO);
        payment.setRefundAmount(BigDecimal.ZERO);
    }

    // =========================================================
    // 1) createPaymentIntent
    // =========================================================

    @Test
    void createPaymentIntent_successfulCreation_expectedPendingPaymentSaved() {
        when(paymentRepository.existsByBookingId(booking.getId())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.createPaymentIntent(payment);

        assertNotNull(result);
        assertEquals(Payment.PaymentStatus.PENDING, result.getStatus());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(result.getTotalAmount()));
        assertEquals("USD", result.getCurrency());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getPaidAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getRequiredAdditionalPaymentAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getPaidAdditionalPaymentAmount()));

        verify(paymentRepository).existsByBookingId(booking.getId());
        verify(paymentRepository).save(payment);
    }

    @Test
    void createPaymentIntent_bookingAlreadyHasPayment_expectedPaymentException() {
        when(paymentRepository.existsByBookingId(booking.getId())).thenReturn(true);

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.createPaymentIntent(payment));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(paymentRepository).existsByBookingId(booking.getId());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createPaymentIntent_bookingCancelled_expectedPaymentException() {
        booking.setStatus(Booking.BookingStatus.CANCELLED);

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.createPaymentIntent(payment));

        assertTrue(ex.getMessage().contains("cancelled booking"));
        verify(paymentRepository, never()).save(any());
    }


    @Test
    void createPaymentIntent_bookingZeroOrNegativeTotalPrice_expectedPaymentException() {
        booking.setTotalPrice(BigDecimal.ZERO);

        PaymentException ex1 = assertThrows(PaymentException.class,
                () -> paymentService.createPaymentIntent(payment));
        assertTrue(ex1.getMessage().contains("Invalid booking amount"));

        booking.setTotalPrice(BigDecimal.valueOf(-1));
        PaymentException ex2 = assertThrows(PaymentException.class,
                () -> paymentService.createPaymentIntent(payment));
        assertTrue(ex2.getMessage().contains("Invalid booking amount"));

        verify(paymentRepository, never()).save(any());
    }


    // =========================================================
    // 2) processPayment
    // =========================================================

    @Test
    void processPayment_paymentFailsDueToSimulateSuccessFalse_expectedFailedStatus() {
        ProcessPaymentRequestDto request = ProcessPaymentRequestDto.builder()
                .paymentIntentId("pi_test_123")
                .simulateSuccess(false)
                .build();

        payment.setStatus(Payment.PaymentStatus.PENDING);
        when(paymentRepository.findByPaymentIntentId("pi_test_123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.processPayment(request);

        assertEquals(Payment.PaymentStatus.FAILED, result.getStatus());
        assertTrue(result.getFailureReason().contains("simulated"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void processPayment_paymentAlreadyProcessedOrCancelled_expectedPaymentException() {
        ProcessPaymentRequestDto request = ProcessPaymentRequestDto.builder()
                .paymentIntentId("pi_test_123")
                .simulateSuccess(true)
                .build();

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        when(paymentRepository.findByPaymentIntentId("pi_test_123")).thenReturn(Optional.of(payment));

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.processPayment(request));

        assertTrue(ex.getMessage().contains("already processed"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPayment_paymentIntentNotFound_expectedPaymentException() {
        ProcessPaymentRequestDto request = ProcessPaymentRequestDto.builder()
                .paymentIntentId("missing_intent")
                .simulateSuccess(true)
                .build();

        when(paymentRepository.findByPaymentIntentId("missing_intent")).thenReturn(Optional.empty());

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.processPayment(request));

        assertTrue(ex.getMessage().contains("not found"));
    }

    // =========================================================
    // 3) processAdditionalPayment
    // =========================================================

    @Test
    void processAdditionalPayment_successfulAdditionalPaymentProcessing_expectedCompletedAndBookingFlagOff() {
        ProcessPaymentRequestDto request = ProcessPaymentRequestDto.builder()
                .paymentIntentId("pi_test_123")
                .simulateSuccess(true)
                .build();

        booking.setAdditionalPaymentRequired(true);
        payment.setStatus(Payment.PaymentStatus.PARTIALLY_PAID);
        payment.setPaidAmount(BigDecimal.valueOf(300));
        payment.setRequiredAdditionalPaymentAmount(BigDecimal.valueOf(200));

        when(paymentRepository.findByPaymentIntentId("pi_test_123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.processAdditionalPayment(request);

        assertEquals(Payment.PaymentStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getPayAdditionalPaymentAt());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(result.getPaidAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getRequiredAdditionalPaymentAmount()));
        assertEquals(0, BigDecimal.valueOf(200).compareTo(result.getPaidAdditionalPaymentAmount()));
        assertFalse(booking.isAdditionalPaymentRequired());

        verify(bookingRepository).save(booking);
    }

    @Test
    void processAdditionalPayment_paymentFailsDueToSimulateSuccessFalse_expectedFailedStatus() {
        ProcessPaymentRequestDto request = ProcessPaymentRequestDto.builder()
                .paymentIntentId("pi_test_123")
                .simulateSuccess(false)
                .build();

        payment.setStatus(Payment.PaymentStatus.PARTIALLY_PAID);
        payment.setRequiredAdditionalPaymentAmount(BigDecimal.valueOf(100));

        when(paymentRepository.findByPaymentIntentId("pi_test_123")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.processAdditionalPayment(request);

        assertEquals(Payment.PaymentStatus.FAILED, result.getStatus());
        assertTrue(result.getFailureReason().contains("simulated"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void processAdditionalPayment_paymentNotInPendingOrPartiallyPaid_expectedPaymentException() {
        ProcessPaymentRequestDto request = ProcessPaymentRequestDto.builder()
                .paymentIntentId("pi_test_123")
                .simulateSuccess(true)
                .build();

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        when(paymentRepository.findByPaymentIntentId("pi_test_123")).thenReturn(Optional.of(payment));

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.processAdditionalPayment(request));

        assertTrue(ex.getMessage().contains("PENDING or PARTIALLY_PAID"));
    }

    @Test
    void processAdditionalPayment_paymentIntentNotFound_expectedPaymentException() {
        ProcessPaymentRequestDto request = ProcessPaymentRequestDto.builder()
                .paymentIntentId("missing_intent")
                .simulateSuccess(true)
                .build();

        when(paymentRepository.findByPaymentIntentId("missing_intent")).thenReturn(Optional.empty());

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.processAdditionalPayment(request));

        assertTrue(ex.getMessage().contains("not found"));
    }

    // =========================================================
    // 4) refundPayment
    // =========================================================

    @Test
    void refundPayment_fullRefundSuccessful_expectedRefundedStatus() {
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAmount(BigDecimal.valueOf(500));
        payment.setRefundAmount(BigDecimal.ZERO);

        RefundRequestDto request = RefundRequestDto.builder()
                .paymentId(10L)
                .refundAmount(BigDecimal.valueOf(500))
                .reason("Full cancellation")
                .build();

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.refundPayment(request);

        assertEquals(Payment.PaymentStatus.REFUNDED, result.getStatus());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(result.getRefundAmount()));
        assertEquals("Full cancellation", result.getRefundReason());
        assertNotNull(result.getRefundedAt());
    }

    @Test
    void refundPayment_partialRefundSuccessful_expectedPartiallyRefundedStatus() {
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAmount(BigDecimal.valueOf(500));
        payment.setRefundAmount(BigDecimal.ZERO);

        RefundRequestDto request = RefundRequestDto.builder()
                .paymentId(10L)
                .refundAmount(BigDecimal.valueOf(200))
                .reason("Partial cancellation")
                .build();

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.refundPayment(request);

        assertEquals(Payment.PaymentStatus.PARTIALLY_REFUNDED, result.getStatus());
        assertEquals(0, BigDecimal.valueOf(200).compareTo(result.getRefundAmount()));
        assertEquals("Partial cancellation", result.getRefundReason());
    }

    @Test
    void refundPayment_refundAmountZeroOrNegative_expectedPaymentException() {
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAmount(BigDecimal.valueOf(500));

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        RefundRequestDto zero = RefundRequestDto.builder()
                .paymentId(10L)
                .refundAmount(BigDecimal.ZERO)
                .reason("Zero")
                .build();

        RefundRequestDto negative = RefundRequestDto.builder()
                .paymentId(10L)
                .refundAmount(BigDecimal.valueOf(-10))
                .reason("Negative")
                .build();

        PaymentException ex1 = assertThrows(PaymentException.class, () -> paymentService.refundPayment(zero));
        PaymentException ex2 = assertThrows(PaymentException.class, () -> paymentService.refundPayment(negative));

        assertTrue(ex1.getMessage().contains("greater than zero"));
        assertTrue(ex2.getMessage().contains("greater than zero"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refundPayment_refundMoreThanPaid_expectedPaymentException() {
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAmount(BigDecimal.valueOf(500));
        payment.setRefundAmount(BigDecimal.valueOf(100));

        RefundRequestDto request = RefundRequestDto.builder()
                .paymentId(10L)
                .refundAmount(BigDecimal.valueOf(450)) // total would be 550 > 500
                .reason("Over refund")
                .build();

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.refundPayment(request));

        assertTrue(ex.getMessage().contains("Cannot refund more than paid amount"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void refundPayment_paymentNotInCompletedOrPartiallyRefunded_expectedPaymentException() {
        payment.setStatus(Payment.PaymentStatus.PENDING);

        RefundRequestDto request = RefundRequestDto.builder()
                .paymentId(10L)
                .refundAmount(BigDecimal.valueOf(100))
                .reason("Invalid status")
                .build();

        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.refundPayment(request));

        assertTrue(ex.getMessage().contains("Cannot refund payment with status"));
    }

    @Test
    void refundPayment_paymentNotFound_expectedPaymentException() {
        RefundRequestDto request = RefundRequestDto.builder()
                .paymentId(999L)
                .refundAmount(BigDecimal.valueOf(100))
                .reason("Not found")
                .build();

        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.refundPayment(request));

        assertTrue(ex.getMessage().contains("Payment not found"));
    }

    // =========================================================
    // 5) Query methods
    // =========================================================

    @Test
    void getPaymentById_found_expectedPaymentReturned() {
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentById(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
    }

    @Test
    void getPaymentById_notFound_expectedPaymentException() {
        when(paymentRepository.findById(10L)).thenReturn(Optional.empty());

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.getPaymentById(10L));

        assertTrue(ex.getMessage().contains("Payment not found"));
    }

    @Test
    void getPaymentByBookingId_found_expectedPaymentReturned() {
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentByBookingId(1L);

        assertNotNull(result);
        assertEquals("pi_test_123", result.getPaymentIntentId());
    }

    @Test
    void getPaymentByBookingId_notFound_expectedPaymentException() {
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.getPaymentByBookingId(1L));

        assertTrue(ex.getMessage().contains("Payment not found for booking"));
    }

    @Test
    void getUserPayments_success_expectedListReturned() {
        Payment p1 = new Payment();
        Payment p2 = new Payment();
        when(paymentRepository.findByBooking_AppUser_Id(77L)).thenReturn(List.of(p1, p2));

        List<Payment> result = paymentService.getUserPayments(77L);

        assertEquals(2, result.size());
        verify(paymentRepository).findByBooking_AppUser_Id(77L);
    }

    @Test
    void getHotelPayments_success_expectedListReturned() {
        Payment p1 = new Payment();
        when(paymentRepository.findByBooking_Hotel_Id(88L)).thenReturn(List.of(p1));

        List<Payment> result = paymentService.getHotelPayments(88L);

        assertEquals(1, result.size());
        verify(paymentRepository).findByBooking_Hotel_Id(88L);
    }
}
