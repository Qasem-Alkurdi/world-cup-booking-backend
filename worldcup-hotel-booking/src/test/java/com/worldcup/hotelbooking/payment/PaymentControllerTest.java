package com.worldcup.hotelbooking.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.booking.booking.BookingServiceImpl;
import com.worldcup.hotelbooking.catalog.hotel.HotelController;
import com.worldcup.hotelbooking.catalog.storage.StaticResourceConfig;
import com.worldcup.hotelbooking.catalog.storage.StorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PaymentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = StaticResourceConfig.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @TestConfiguration
    static class TestBeans {
        @Bean
        StorageProperties storageProperties() {
            return new StorageProperties();
        }
    }


    @Autowired
    private MockMvc mockMvc;


    private ObjectMapper objectMapper= new ObjectMapper();


    @MockitoBean
    private PaymentServiceImpl paymentService;

    @MockitoBean
    private BookingServiceImpl bookingService;

    // ---------------------------
    // Helpers
    // ---------------------------

    private Booking sampleBooking() {
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("WC2026-BOOK-001");
        booking.setCheckInDate(LocalDate.of(2026, 7, 1));
        booking.setCheckOutDate(LocalDate.of(2026, 7, 5));
        booking.setNumberOfGuests(2);
        booking.setNumberOfAdults(2);
        booking.setNumberOfChildren(0);
        booking.setTotalPrice(BigDecimal.valueOf(500));
        booking.setStatus(Booking.BookingStatus.PENDING);
        return booking;
    }

    private Payment samplePayment(Payment.PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(10L);
        payment.setPaymentIntentId("pi_test_123");
        payment.setTransactionReference("txn_test_123");
        payment.setStatus(status);
        payment.setPaymentMethod(Payment.PaymentMethod.CREDIT_CARD);
        payment.setTotalAmount(BigDecimal.valueOf(500));
        payment.setPaidAmount(BigDecimal.valueOf(500));
        payment.setRefundAmount(BigDecimal.ZERO);
        payment.setCurrency("USD");
        payment.setBooking(sampleBooking());
        return payment;
    }

    private ProcessPaymentRequestDto processRequest(boolean simulateSuccess) {
        return ProcessPaymentRequestDto.builder()
                .paymentIntentId("pi_test_123")
                .simulateSuccess(simulateSuccess)
                .build();
    }

    // =====================================================
    // 1) POST /payments/create-intent
    // =====================================================

    @Test
    @DisplayName("POST /payments/create-intent - success -> 201 created")
    void createIntent_successfulCreation_expectedCreated() throws Exception {
        PaymentIntentRequestDto request = PaymentIntentRequestDto.builder()
                .bookingId(1L)
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                .build();

        when(bookingService.getBookingById(1L)).thenReturn(sampleBooking());
        when(paymentService.createPaymentIntent(any(Payment.class))).thenReturn(samplePayment(Payment.PaymentStatus.PENDING));

        mockMvc.perform(post("/payments/create-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentIntentId").value("pi_test_123"));
    }

    @Test
    @DisplayName("POST /payments/create-intent - booking not found -> error")
    void createIntent_bookingNotFound_expectedErrorStatus() throws Exception {
        PaymentIntentRequestDto request = PaymentIntentRequestDto.builder()
                .bookingId(99L)
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                .build();

        when(bookingService.getBookingById(99L)).thenThrow(new RuntimeException("Booking not found"));

        jakarta.servlet.ServletException ex = assertThrows(
                jakarta.servlet.ServletException.class,
                () -> mockMvc.perform(post("/payments/create-intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
        );

        assertTrue(ex.getCause() instanceof RuntimeException);
        assertEquals("Booking not found", ex.getCause().getMessage());

    }

    // =====================================================
    // 2) POST /payments/process
    // =====================================================

    @Test
    @DisplayName("POST /payments/process - payment success -> 200")
    void processPayment_success_expectedOk() throws Exception {
        ProcessPaymentRequestDto request = processRequest(true);
        when(paymentService.processPayment(any(ProcessPaymentRequestDto.class)))
                .thenReturn(samplePayment(Payment.PaymentStatus.COMPLETED));

        mockMvc.perform(post("/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /payments/process - simulate fail -> 402")
    void processPayment_failed_expectedPaymentRequired() throws Exception {
        ProcessPaymentRequestDto request = processRequest(false);
        when(paymentService.processPayment(any(ProcessPaymentRequestDto.class)))
                .thenReturn(samplePayment(Payment.PaymentStatus.FAILED));

        mockMvc.perform(post("/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    @DisplayName("POST /payments/process - payment intent not found -> error")
    void processPayment_intentNotFound_expectedErrorStatus() throws Exception {
        ProcessPaymentRequestDto request = processRequest(true);

        when(paymentService.processPayment(any(ProcessPaymentRequestDto.class)))
                .thenThrow(new PaymentException("Payment intent not found"));

        mockMvc.perform(post("/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // =====================================================
    // 3) POST /payments/additional-payment
    // =====================================================

    @Test
    @DisplayName("POST /payments/additional-payment - success -> 200")
    void additionalPayment_success_expectedOk() throws Exception {
        ProcessPaymentRequestDto request = processRequest(true);
        when(paymentService.processAdditionalPayment(any(ProcessPaymentRequestDto.class)))
                .thenReturn(samplePayment(Payment.PaymentStatus.COMPLETED));

        mockMvc.perform(post("/payments/additional-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /payments/additional-payment - failed -> 402")
    void additionalPayment_failed_expectedPaymentRequired() throws Exception {
        ProcessPaymentRequestDto request = processRequest(false);
        when(paymentService.processAdditionalPayment(any(ProcessPaymentRequestDto.class)))
                .thenReturn(samplePayment(Payment.PaymentStatus.FAILED));

        mockMvc.perform(post("/payments/additional-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    @DisplayName("POST /payments/additional-payment - invalid status -> error")
    void additionalPayment_invalidState_expectedErrorStatus() throws Exception {
        ProcessPaymentRequestDto request = processRequest(true);
        when(paymentService.processAdditionalPayment(any(ProcessPaymentRequestDto.class)))
                .thenThrow(new PaymentException("Payment must be PENDING or PARTIALLY_PAID"));

        mockMvc.perform(post("/payments/additional-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // =====================================================
    // 4) POST /payments/refund
    // =====================================================

    @Test
    @DisplayName("POST /payments/refund - manual refund success -> 200")
    void refund_success_expectedOk() throws Exception {
        RefundRequestDto request = RefundRequestDto.builder()
                .paymentId(10L)
                .refundAmount(BigDecimal.valueOf(200))
                .reason("Partial refund")
                .build();

        Payment refunded = samplePayment(Payment.PaymentStatus.PARTIALLY_REFUNDED);
        refunded.setRefundAmount(BigDecimal.valueOf(200));

        when(paymentService.refundPayment(any(RefundRequestDto.class))).thenReturn(refunded);

        mockMvc.perform(post("/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newStatus").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Refund processed successfully"));

    }

    @Test
    @DisplayName("POST /payments/refund - payment not found -> error")
    void refund_paymentNotFound_expectedErrorStatus() throws Exception {
        RefundRequestDto request = RefundRequestDto.builder()
                .paymentId(999L)
                .refundAmount(BigDecimal.valueOf(100))
                .reason("Not found")
                .build();

        when(paymentService.refundPayment(any(RefundRequestDto.class)))
                .thenThrow(new PaymentException("Payment not found"));

        mockMvc.perform(post("/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /payments/refund - over refund -> error")
    void refund_overRefund_expectedErrorStatus() throws Exception {
        RefundRequestDto request = RefundRequestDto.builder()
                .paymentId(10L)
                .refundAmount(BigDecimal.valueOf(9999))
                .reason("Over refund")
                .build();

        when(paymentService.refundPayment(any(RefundRequestDto.class)))
                .thenThrow(new PaymentException("Cannot refund more than paid amount"));

        mockMvc.perform(post("/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /payments/refund - invalid payment status -> error")
    void refund_invalidStatus_expectedErrorStatus() throws Exception {
        RefundRequestDto request = RefundRequestDto.builder()
                .paymentId(10L)
                .refundAmount(BigDecimal.valueOf(100))
                .reason("Invalid status")
                .build();

        when(paymentService.refundPayment(any(RefundRequestDto.class)))
                .thenThrow(new PaymentException("Cannot refund payment with status"));

        mockMvc.perform(post("/payments/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // =====================================================
    // 5) GET /payments/{id}
    // =====================================================

    @Test
    @DisplayName("GET /payments/{id} - success -> 200")
    void getPaymentById_found_expectedOk() throws Exception {
        when(paymentService.getPaymentById(10L)).thenReturn(samplePayment(Payment.PaymentStatus.COMPLETED));

        mockMvc.perform(get("/payments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("GET /payments/{id} - not found -> error")
    void getPaymentById_notFound_expectedErrorStatus() throws Exception {
        when(paymentService.getPaymentById(10L)).thenThrow(new PaymentException("Payment not found"));

        mockMvc.perform(get("/payments/10"))
                .andExpect(status().is4xxClientError());
    }

    // =====================================================
    // 6) GET /payments/booking/{bookingId}
    // =====================================================

    @Test
    @DisplayName("GET /payments/booking/{bookingId} - success -> 200")
    void getPaymentByBookingId_found_expectedOk() throws Exception {
        when(paymentService.getPaymentByBookingId(1L)).thenReturn(samplePayment(Payment.PaymentStatus.COMPLETED));

        mockMvc.perform(get("/payments/booking/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentIntentId").value("pi_test_123"));
    }

    @Test
    @DisplayName("GET /payments/booking/{bookingId} - payment not found -> error")
    void getPaymentByBookingId_notFound_expectedErrorStatus() throws Exception {
        when(paymentService.getPaymentByBookingId(1L))
                .thenThrow(new PaymentException("Payment not found for booking"));

        mockMvc.perform(get("/payments/booking/1"))
                .andExpect(status().is4xxClientError());
    }

    // =====================================================
    // 7) GET /payments/user/{userId}
    // =====================================================

    @Test
    @DisplayName("GET /payments/user/{userId} - success -> 200")
    void getUserPayments_success_expectedOk() throws Exception {
        when(paymentService.getUserPayments(7L))
                .thenReturn(List.of(samplePayment(Payment.PaymentStatus.COMPLETED)));

        mockMvc.perform(get("/payments/user/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    // =====================================================
    // 8) GET /payments/hotel/{hotelId}
    // =====================================================

    @Test
    @DisplayName("GET /payments/hotel/{hotelId} - success -> 200")
    void getHotelPayments_success_expectedOk() throws Exception {
        when(paymentService.getHotelPayments(8L))
                .thenReturn(List.of(samplePayment(Payment.PaymentStatus.PARTIALLY_REFUNDED)));

        mockMvc.perform(get("/payments/hotel/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PARTIALLY_REFUNDED"));
    }
}