package com.worldcup.hotelbooking.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.hotelbooking.catalog.storage.StaticResourceConfig;
import com.worldcup.hotelbooking.catalog.storage.StorageProperties;
import com.worldcup.hotelbooking.reservation.booking.Booking;
import com.worldcup.hotelbooking.reservation.booking.BookingServiceImpl;
import com.worldcup.hotelbooking.security.JwtTokenService;
import com.worldcup.hotelbooking.security.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PaymentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = StaticResourceConfig.class
        ),
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PaymentServiceImpl paymentService;
    @MockitoBean
    private BookingServiceImpl bookingService;
    @MockitoBean
    private RateLimitService rateLimitService;
    @MockitoBean
    private JwtTokenService jwtTokenService;
    @MockitoBean                          // ← add this
    private CacheManager cacheManager;

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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.paymentIntentId").value("pi_test_123"));
    }

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
    @DisplayName("POST /payments/process - payment intent not found -> 4xx")
    void processPayment_intentNotFound_expectedClientError() throws Exception {
        ProcessPaymentRequestDto request = processRequest(true);
        when(paymentService.processPayment(any(ProcessPaymentRequestDto.class)))
                .thenThrow(new PaymentException("Payment intent not found"));

        mockMvc.perform(post("/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

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
    @DisplayName("POST /payments/additional-payment - invalid state -> 4xx")
    void additionalPayment_invalidState_expectedClientError() throws Exception {
        ProcessPaymentRequestDto request = processRequest(true);
        when(paymentService.processAdditionalPayment(any(ProcessPaymentRequestDto.class)))
                .thenThrow(new PaymentException("Payment must be PENDING or PARTIALLY_PAID"));

        mockMvc.perform(post("/payments/additional-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /payments/refund - success -> 200 with refund response payload")
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.paymentId").value(10))
                .andExpect(jsonPath("$.newStatus").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Refund processed successfully"));
    }

    @Test
    @DisplayName("POST /payments/refund - payment not found -> 4xx")
    void refund_paymentNotFound_expectedClientError() throws Exception {
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
    @DisplayName("POST /payments/refund - over refund -> 4xx")
    void refund_overRefund_expectedClientError() throws Exception {
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
    @DisplayName("POST /payments/refund - invalid status -> 4xx")
    void refund_invalidStatus_expectedClientError() throws Exception {
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

    @Test
    @DisplayName("GET /payments/{id} - success -> 200")
    void getPaymentById_found_expectedOk() throws Exception {
        when(paymentService.getPaymentById(10L)).thenReturn(samplePayment(Payment.PaymentStatus.COMPLETED));

        mockMvc.perform(get("/payments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("GET /payments/{id} - not found -> 4xx")
    void getPaymentById_notFound_expectedClientError() throws Exception {
        when(paymentService.getPaymentById(10L)).thenThrow(new PaymentException("Payment not found"));

        mockMvc.perform(get("/payments/10"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("GET /payments/booking/{bookingId} - success -> 200")
    void getPaymentByBookingId_found_expectedOk() throws Exception {
        when(paymentService.getPaymentByBookingId(1L)).thenReturn(samplePayment(Payment.PaymentStatus.COMPLETED));

        mockMvc.perform(get("/payments/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentIntentId").value("pi_test_123"));
    }

    @Test
    @DisplayName("GET /payments/booking/{bookingId} - not found -> 4xx")
    void getPaymentByBookingId_notFound_expectedClientError() throws Exception {
        when(paymentService.getPaymentByBookingId(1L))
                .thenThrow(new PaymentException("Payment not found for booking"));

        mockMvc.perform(get("/payments/booking/1"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("GET /payments/user/{userId} - success -> 200")
    void getUserPayments_success_expectedOk() throws Exception {
        Page<Payment> page = new PageImpl<>(
                List.of(samplePayment(Payment.PaymentStatus.COMPLETED)),
                PageRequest.of(0, 20),
                1
        );
        when(paymentService.getUserPayments(eq(7L), any()))
                .thenReturn(page);

        mockMvc.perform(get("/payments/users/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    @DisplayName("GET /payments/hotel/{hotelId} - success -> 200")
    void getHotelPayments_success_expectedOk() throws Exception {
        Page<Payment> page = new PageImpl<>(
                List.of(samplePayment(Payment.PaymentStatus.PARTIALLY_REFUNDED)),
                PageRequest.of(0, 20),
                1
        );
        when(paymentService.getHotelPayments(eq(8L), any()))
                .thenReturn(page);

        mockMvc.perform(get("/payments/hotels/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PARTIALLY_REFUNDED"));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        StorageProperties storageProperties() {
            return new StorageProperties();
        }
    }
}