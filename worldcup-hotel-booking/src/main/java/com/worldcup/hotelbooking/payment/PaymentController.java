package com.worldcup.hotelbooking.payment;

import com.worldcup.hotelbooking.availability_pricing.pricing.PricingResponseDto;
import com.worldcup.hotelbooking.booking.booking.BookingServiceImpl;
import com.worldcup.hotelbooking.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentServiceImpl paymentService;
    private final BookingServiceImpl bookingService;


    public PaymentController(PaymentServiceImpl paymentService, BookingServiceImpl bookingService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;

    }

    /**
     * Step 1: Create a payment intent for a booking
     *
     * POST /payments/create-intent
     */
    @PostMapping("/create-intent")
    @Operation(summary = "Create payment intent",
            description = "Create a payment intent for a booking. This is step 1 before processing payment.")
    @PreAuthorize("@paymentAuthorizationService.canCreatPayment(#request.bookingId, authentication)")
    public ResponseEntity<PaymentIntentResponseDto> createPaymentIntent(
            @Valid @RequestBody PaymentIntentRequestDto request) {

        logger.info("Creating payment intent for booking: {}", request.getBookingId());

        PaymentIntentResponseDto response =PaymentMapper.toPaymentIntentDto(paymentService.createPaymentIntent(PaymentMapper.toEntity(request, bookingService.getBookingById(request.getBookingId()))));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========================================
    // PROCESS PAYMENT
    // ========================================

    /**
     * Step 2: Process the payment (mock payment gateway)
     *
     * POST /payments/process
     */
    @PostMapping("/process")
    @Operation(summary = "Process payment",
            description = "Process payment for a payment intent. MOCK: Set simulateSuccess=false to test failure.")
   @PreAuthorize("@paymentAuthorizationService.canProcessPayment(#request.getPaymentIntentId(), authentication)")
    public ResponseEntity<ProcessPaymentResponseDto> processPayment(
            @Valid @RequestBody ProcessPaymentRequestDto request) {

        logger.info("Processing payment for intent: {}", request.getPaymentIntentId());

        ProcessPaymentResponseDto response = PaymentMapper.toProcessPaymentResponseDto(paymentService.processPayment(request));

        if (response.getStatus() == Payment.PaymentStatus.COMPLETED) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
        }
    }


    @PostMapping("/additional-payment")
    @Operation(summary = "Process additional payment for a booking",
            description = "Process an additional payment for a booking, such as for extra services or late check-out.")
        @PreAuthorize("@paymentAuthorizationService.canProcessPayment(#request.getPaymentIntentId(), authentication)")
    public ResponseEntity<ProcessAddiPaymentResponseDto> processAdditionalPayment(
            @Valid @RequestBody ProcessPaymentRequestDto request) {
        logger.info("Processing additional payment for intent: {}", request.getPaymentIntentId());
       ProcessAddiPaymentResponseDto response = PaymentMapper.processAddiPaymentResponseDto(paymentService.processAdditionalPayment(request));
        if (response.getStatus() == Payment.PaymentStatus.COMPLETED) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
        }
    }

    // ========================================
    // REFUND PAYMENT
    // ========================================

    // ========================================
// UPDATED PaymentController.java
// Manual refund is for ADMIN/MANAGER ONLY
// ========================================

    /**
     * Manual refund endpoint - ADMIN/MANAGER ONLY
     *
     * This is for SPECIAL CASES like:
     * - Customer service gestures
     * - Dispute resolutions
     * - Compensation
     *
     * For NORMAL booking cancellations, refund is AUTOMATIC via cancelBooking()
     *
     * POST /payments/refund
     */
    @PostMapping("/refund")
    @Operation(
            summary = "Manual refund (Admin/Manager only)",
            description = "Process a manual refund for special cases. " +
                    "Normal cancellations use automatic refund via booking cancellation."
    )
  @PreAuthorize("hasRole('ADMIN') or(hasRole('MANAGER') and @paymentAuthorizationService.canRefundPayment(#request.getPaymentId(),authentication) )") // Only ADMIN/MANAGER can access this endpoint
    public ResponseEntity<RefundResponseDto> manualRefund(
            @Valid @RequestBody RefundRequestDto request) {

        logger.info("⚠️ MANUAL refund requested for payment: {}", request.getPaymentId());

        RefundResponseDto response = PaymentMapper.toRefundResponseDto(paymentService.refundPayment(request));

        return ResponseEntity.ok(response);
    }

    // ========================================
    // QUERY ENDPOINTS
    // ========================================

    /**
     * Get payment by ID
     *
     * GET /payments/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    @PreAuthorize("hasRole('ADMIN') or @paymentAuthorizationService.canViewPayment(#id, authentication)")
    public ResponseEntity<PaymentResponseDto> getPaymentById(@PathVariable Long id) {
        PaymentResponseDto response = PaymentMapper.toPaymentResponseDto(paymentService.getPaymentById(id));
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment by booking ID
     *
     * GET /payments/booking/{bookingId}
     */
    @GetMapping("/bookings/{bookingId}")
    @Operation(summary = "Get payment by booking ID")
    @PreAuthorize("hasRole('ADMIN') or @paymentAuthorizationService.canViewPaymentByBookingId(#bookingId, authentication)")
    public ResponseEntity<PaymentResponseDto> getPaymentByBookingId(@PathVariable Long bookingId) {
        PaymentResponseDto response = PaymentMapper.toPaymentResponseDto(paymentService.getPaymentByBookingId(bookingId));
        return ResponseEntity.ok(response);
    }

    /**
     * Get all payments for a user
     *
     * GET /payments/user/{userId}
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user's payment history")
    @PreAuthorize("hasRole('ADMIN') or @paymentAuthorizationService.isCurrentUser(#userId, authentication)")
    public PagedResponse<PaymentResponseDto> getUserPayments(@PathVariable Long userId,
                                                             @PageableDefault(size = 10, sort = "totalAmount") Pageable pageable) {
        Page<Payment> paymentsPage = paymentService.getUserPayments(userId, pageable);
        List<PaymentResponseDto> payments = paymentsPage
                                           .getContent()
                                           .stream().map(PaymentMapper::toPaymentResponseDto).toList();
        return PagedResponse.from(paymentsPage, payments);

    }

    /**
     * Get all payments for a hotel
     *
     * GET /payments/hotel/{hotelId}
     */
    @GetMapping("/hotels/{hotelId}")
    @Operation(summary = "Get hotel's payment history")
    @PreAuthorize("hasRole('ADMIN') or @paymentAuthorizationService.isHimTheHotelOwnerOfTheBookings(#hotelId, authentication)")
    public PagedResponse<PaymentResponseDto> getHotelPayments(@PathVariable Long hotelId,
                                                              @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        Page<Payment> paymentsPage = paymentService.getHotelPayments(hotelId, pageable);
        List<PaymentResponseDto> payments = paymentsPage
                .getContent()
                .stream().map(PaymentMapper::toPaymentResponseDto).toList();

        return PagedResponse.from(paymentsPage, payments);
    }
}