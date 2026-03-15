package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.availability_pricing.availability.AvailabilityServiceImpl;
import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingServiceImpl;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.cancellation.CancellationPolicyServiceImpl;
import com.worldcup.hotelbooking.booking.cancellation.CancellationResponseDto;
import com.worldcup.hotelbooking.payment.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


@Service
@Transactional
public class BookingServiceImpl implements BookingService {
    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImpl.class);
    private final BookingRepository bookingRepository;
    private final EnhancedPricingServiceImpl enhancedPricingService;
    private final CancellationPolicyServiceImpl cancellationPolicyService;
    private final AvailabilityServiceImpl availabilityService;
    private final PaymentRepository paymentRepository;
    private final PaymentServiceImpl paymentService;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            EnhancedPricingServiceImpl enhancedPricingService,
            CancellationPolicyServiceImpl cancellationPolicyService,
            AvailabilityServiceImpl availabilityService,
            PaymentRepository paymentRepository,
            PaymentServiceImpl paymentService) {
        this.bookingRepository = bookingRepository;
        this.enhancedPricingService = enhancedPricingService;
        this.cancellationPolicyService = cancellationPolicyService;
        this.availabilityService = availabilityService;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    //get
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findByIdWithRooms(id).orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
    }

    /// //////////////////////////////////////////////////////////

    //create
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
//to run all the code in this method as a single transaction and to prevent dirty reads, non-repeatable reads, and phantom reads, ensuring data integrity during the booking process.
    public Booking createBooking(Booking booking) {

        booking.setStatus(Booking.BookingStatus.PENDING);
        if (booking.getCheckOutDate().isBefore(booking.getCheckInDate())) {
            throw new IllegalArgumentException("Check-out date cannot be before check-in date");
        }
        if (booking.getBookingRooms() == null || booking.getBookingRooms().isEmpty()) {
            throw new IllegalArgumentException("At least one room must be booked");
        }
        if (!availabilityService.isNumberOfGuestsValid(booking)) {
            throw new IllegalArgumentException("Number of guests exceeds room capacity");
        }

        if (booking.getNumberOfGuests() != booking.getNumberOfAdults() + booking.getNumberOfChildren()) {
            throw new IllegalArgumentException("Total number of guests must equal the sum of adults and children");
        }
        for (BookingRoom room : booking.getBookingRooms()) {
            if (!availabilityService.checkAvailability(room.getRoomType().getId(), booking.getCheckInDate(), booking.getCheckOutDate(), room.getNumberOfRooms())) {
                throw new IllegalArgumentException("Not enough rooms available for room type: " + room.getRoomType().getName());
            }
        }

        booking.setTotalPrice(calculateTotalPrice(booking));
        booking.setConfirmationDeadline(LocalDateTime.now().plusDays(3));



        return bookingRepository.save(booking);
    }

    public BigDecimal calculateTotalPrice(Booking booking) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (BookingRoom room : booking.getBookingRooms()) {
            BigDecimal roomPrice = enhancedPricingService.calculateTotalStayPrice(booking, room.getRoomType().getHotel(), room.getRoomType(), room.getNumberOfRooms());
            totalPrice = totalPrice.add(roomPrice);
            room.setBasePricePerNightPerRoom(room.getRoomType().getBasePrice());
            room.setTotalPriceWithFees(roomPrice);

        }
        return totalPrice.setScale(2, RoundingMode.HALF_UP);
    }


    @Override
    @Transactional
    public Booking cancelBooking(Long id, String reason) {
        logger.info("Cancelling booking with id: {} for reason: {}", id, reason);

        Booking booking = bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        // CHECK CANCELLATION POLICY
        CancellationResponseDto cancellationResult = cancellationPolicyService.previewCancellation(booking);

        if (!cancellationResult.isCanCancel()) {
            throw new IllegalStateException(cancellationResult.getPolicyMessage());
        }

        // Log refund information
        logger.info("Cancellation approved: Refund ${} ({}%), Fee ${}",
                cancellationResult.getRefundAmount(),
                cancellationResult.getRefundPercentage(),
                cancellationResult.getCancellationFee());

        if (paymentRepository.existsByBookingId(booking.getId())) {
            Payment payment = paymentRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new PaymentException("Payment not found"));

            // Allow refund for COMPLETED or PARTIALLY_REFUNDED (e.g. after a price-decrease modification)
            boolean canRefund = payment.getStatus() == Payment.PaymentStatus.COMPLETED
                    || payment.getStatus() == Payment.PaymentStatus.PARTIALLY_REFUNDED;

            if (canRefund && cancellationResult.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {

                // Calculate how much is still left to refund (total paid minus already refunded)
                BigDecimal alreadyRefunded = payment.getRefundAmount() != null
                        ? payment.getRefundAmount()
                        : BigDecimal.ZERO;
                BigDecimal paidAmount = payment.getPaidAmount() != null
                        ? payment.getPaidAmount()
                        : BigDecimal.ZERO;
                BigDecimal maxRefundable = paidAmount.subtract(alreadyRefunded);

                BigDecimal cancellationRefund = cancellationResult.getRefundAmount().min(maxRefundable);

                if (cancellationRefund.compareTo(BigDecimal.ZERO) > 0) {
                    // ✅ Refund amount comes from CANCELLATION POLICY (capped at remaining paid amount)
                    RefundRequestDto refundRequest = RefundRequestDto.builder()
                            .paymentId(payment.getId())
                            .refundAmount(cancellationRefund)
                            .reason(reason + " | " + cancellationResult.getPolicyMessage())
                            .build();

                    paymentService.refundPayment(refundRequest);
                }
            }
        }

        // Update booking status
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelReason(reason + " | " + cancellationResult.getPolicyMessage());
        booking.setCancelledAt(java.time.LocalDateTime.now());
        booking.setCancelledBy(booking.getAppUser().getUsername());

        // You might want to add refund fields to Booking entity:
        // booking.setRefundAmount(cancellationResult.getRefundAmount());
        // booking.setCancellationFee(cancellationResult.getCancellationFee());

        Booking cancelled = bookingRepository.save(booking);
        logger.info("Booking {} cancelled successfully - Refund: ${}",
                cancelled.getBookingReference(),
                cancellationResult.getRefundAmount());

        return cancelled;
    }

    /**
     * Preview cancellation without actually cancelling
     * Shows user what refund they would get
     */
    public CancellationResponseDto previewCancellation(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));

        return cancellationPolicyService.previewCancellation(booking);
    }

    @Override
    public Booking confirmBooking(Long id) {
        Booking booking = bookingRepository.findByIdWithRooms(id).orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking is already confirmed");
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled booking cannot be confirmed");
        }
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setConfirmedAt(java.time.LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    public void addBookingRoom(BookingRoom bookingRoom) {
        Booking booking = bookingRoom.getBooking();
        booking.getBookingRooms().add(bookingRoom);
        bookingRoom.setBooking(booking);
    }

    public Booking findBookingByReference(String bookingReference) {
        return bookingRepository.findByBookingReference(bookingReference).orElseThrow(() -> new BookingNotFoundException("Booking not found with reference: " + bookingReference));
    }

    @Transactional
    public Booking updateExisting(long id, Booking requestBooking) {
        // 1. Fetch the MANAGED entity with its rooms
        Booking managedBooking = bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + id));

        // 2. Business Rule Validations
        validateCanModify(managedBooking, requestBooking);

        // 3. Store old values for comparison
        BigDecimal oldPrice = managedBooking.getTotalPrice();
        Booking.BookingStatus oldStatus = managedBooking.getStatus();
        // 3. Update top-level fields
        managedBooking.setCheckInDate(requestBooking.getCheckInDate());
        managedBooking.setCheckOutDate(requestBooking.getCheckOutDate());
        managedBooking.setNumberOfGuests(requestBooking.getNumberOfGuests());
        managedBooking.setNumberOfAdults(requestBooking.getNumberOfAdults());
        managedBooking.setNumberOfChildren(requestBooking.getNumberOfChildren());

        // 4. SMART ROOM UPDATE: Synchronize the collections
        // This avoids deleting and re-inserting the same rooms
        BigDecimal newTotal = updateBookingRoomsAndPrice(managedBooking, requestBooking.getBookingRooms());
        managedBooking.setTotalPrice(newTotal);
        // 5. DATA INTEGRITY: Validate logic (dates, capacity, etc.)
        performBookingValidations(managedBooking);

        // 8. ⭐ SMART PAYMENT HANDLING
        handlePriceChange(managedBooking, oldStatus, oldPrice, newTotal);


        return bookingRepository.save(managedBooking);
    }

    private BigDecimal updateBookingRoomsAndPrice(Booking managed, List<BookingRoom> requestedRooms) {
        // Simple strategy: If the rooms are complex, clear is okay ONLY IF
        // you calculate prices IMMEDIATELY after adding them.
        managed.getBookingRooms().clear();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (BookingRoom room : requestedRooms) {
            BigDecimal roomPrice = enhancedPricingService.calculateTotalStayPrice(managed, room.getRoomType().getHotel(), room.getRoomType(), room.getNumberOfRooms());
            totalPrice = totalPrice.add(roomPrice);
            room.setBasePricePerNightPerRoom(room.getRoomType().getBasePrice());
            room.setTotalPriceWithFees(roomPrice);
            room.setBooking(managed);
            managed.getBookingRooms().add(room);
        }
        return totalPrice.setScale(2, RoundingMode.HALF_UP);
    }


    private void performBookingValidations(Booking booking) {
        if (booking.getCheckOutDate().isBefore(booking.getCheckInDate())) {
            throw new IllegalArgumentException("Check-out cannot be before check-in");
        }
        if (!availabilityService.isNumberOfGuestsValid(booking)) {
            throw new IllegalArgumentException("Guests exceed capacity");
        }
        // Check availability for the new dates/rooms
        for (BookingRoom room : booking.getBookingRooms()) {
            boolean available = availabilityService.checkAvailability(
                    room.getRoomType().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    room.getNumberOfRooms()
            );
            if (!available) {
                throw new IllegalArgumentException("Room type " + room.getRoomType().getName() + " is full for these dates.");
            }
        }
    }

    /**
     * Validate booking can be modified
     */
    private void validateCanModify(Booking booking, Booking request) {

        if (booking.getHotel().getId() != request.getHotel().getId())
            throw new ModificationNotAllowedException(
                    "Cannot modify the hotel"
            );

        if (booking.getStatus() == Booking.BookingStatus.CHECKED_IN) {
            throw new ModificationNotAllowedException(
                    "Cannot modify after check-in. Contact reception.");
        }

        if (booking.getStatus() == Booking.BookingStatus.CHECKED_OUT) {
            throw new ModificationNotAllowedException(
                    "Cannot modify completed booking");
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new ModificationNotAllowedException(
                    "Cannot modify cancelled booking. Create new booking.");
        }

        if (booking.getCheckInDate().isBefore(LocalDate.now())) {
            throw new ModificationNotAllowedException(
                    "Cannot modify after check-in date passed");
        }
    }

    private List<String> analyzeChanges(Booking original, Booking request) {

        List<String> changes = new ArrayList<>();

        if (!original.getCheckInDate().equals(request.getCheckInDate())) {
            changes.add(String.format("Check-in: %s → %s",
                    original.getCheckInDate(), request.getCheckInDate()));
        }

        if (!original.getCheckOutDate().equals(request.getCheckOutDate())) {
            changes.add(String.format("Check-out: %s → %s",
                    original.getCheckOutDate(), request.getCheckOutDate()));
        }


        if (original.getBookingRooms().size() != request.getBookingRooms().size()) {
            changes.add(String.format("Rooms: %d → %d",
                    original.getBookingRooms().size(), request.getBookingRooms().size()));
        }

        if (original.getNumberOfGuests() != request.getNumberOfGuests()) {
            changes.add(String.format("Guests: %d → %d",
                    original.getNumberOfGuests(), request.getNumberOfGuests()));
        }

        return changes;
    }


    /**
     * Validate changes are allowed
     */
    private void validateChanges(List<String> analysis) {
        if (analysis.isEmpty()) {
            throw new ModificationNotAllowedException("No changes detected");
        }
    }


    public Page<Booking> getGuestHistory(
            Long userId,
            Pageable pageable
    ) {
        Specification<Booking> spec =
                Specification.where(BookingSpecifications.hasUser(userId))
                        .and(BookingSpecifications.isPast());

        return bookingRepository.findAll(spec, pageable);
    }

    public Page<Booking> getHotelUpcomingBookings(
            Long hotelId,
            Pageable pageable
    ) {
        Specification<Booking> spec =
                Specification.where(BookingSpecifications.hasHotel(hotelId))
                        .and(BookingSpecifications.isUpcoming())
                        .and(BookingSpecifications.hasStatus(Booking.BookingStatus.CONFIRMED));

        return bookingRepository.findAll(spec, pageable);
    }

    public Page<Booking> filterBookings(
            Long userId,
            Long hotelId,
            Booking.BookingStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Double minPrice,
            Double maxPrice,
            Pageable pageable
    ) {

        Specification<Booking> spec = Specification.where((root, query, criteriaBuilder) -> criteriaBuilder.conjunction());

        if (userId != null)
            spec = spec.and(BookingSpecifications.hasUser(userId));

        if (hotelId != null)
            spec = spec.and(BookingSpecifications.hasHotel(hotelId));

        spec = spec.and(BookingSpecifications.hasStatus(status))
                .and(BookingSpecifications.checkInAfter(fromDate))
                .and(BookingSpecifications.checkOutBefore(toDate))
                .and(BookingSpecifications.priceBetween(minPrice, maxPrice));

        return bookingRepository.findAll(spec, pageable);
    }


    public Booking checkInBooking(Long id) {
        Booking booking = bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        // ⭐ VALIDATE PAYMENT STATUS
        if (!booking.canCheckIn()) {
            if (booking.isAdditionalPaymentRequired()) {
                throw new IllegalStateException(
                        "Cannot check in - Additional payment is required. Please complete payment before check-in."
                );
            }
        }

        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be checked in");
        }

        if (booking.getCheckInDate().isAfter(LocalDate.now())) {
            throw new IllegalStateException("Cannot check in before check-in date");
        }

        booking.setStatus(Booking.BookingStatus.CHECKED_IN);
        logger.info("✅ Check-in successful");

        return bookingRepository.save(booking);

    }

    public Booking checkOutBooking(Long id) {
        Booking booking = bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() != Booking.BookingStatus.CHECKED_IN) {
            throw new IllegalStateException("Only checked-in bookings can be checked out");
        }


        booking.setStatus(Booking.BookingStatus.CHECKED_OUT);
        return bookingRepository.save(booking);
    }


    // ========================================
    // PRICE CHANGE HANDLER
    // ========================================

    private void handlePriceChange(Booking booking,
                                   Booking.BookingStatus oldStatus,
                                   BigDecimal oldPrice,
                                   BigDecimal newPrice) {

        int priceComparison = newPrice.compareTo(oldPrice);

        if (priceComparison == 0) {
            logger.info("✅ Price unchanged: ${}", newPrice);
            return;
        }

        if (oldStatus == Booking.BookingStatus.PENDING) {
            // ✅ PENDING: Just update price, no payment processing
            logger.info("📝 PENDING booking - Price updated: ${} → ${}", oldPrice, newPrice);

        } else if (oldStatus == Booking.BookingStatus.CONFIRMED) {

            if (priceComparison < 0) {
                // 💰 Price DECREASED: Refund difference
                handlePriceDecrease(booking, oldPrice, newPrice);

            } else {
                // 💳 Price INCREASED: Additional payment required
                handlePriceIncrease(booking, oldPrice, newPrice);
            }
        }
    }

    // ========================================
    // PRICE DECREASE: REFUND WITH POLICY
    // ========================================

    private void handlePriceDecrease(Booking booking, BigDecimal oldPrice, BigDecimal newPrice) {
        BigDecimal priceDifference = oldPrice.subtract(newPrice);

        logger.info("💰 Price decreased by ${}", priceDifference);

        // Calculate refund based on cancellation policy
        long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), booking.getCheckInDate());

        BigDecimal refundAmount;
        int refundPercentage;
        String refundPolicy;

        if (daysUntilCheckIn >= 30) {
            refundPercentage = 100;
            refundAmount = priceDifference;
            refundPolicy = "Full refund - 30+ days notice";
        } else if (daysUntilCheckIn >= 14) {
            refundPercentage = 75;
            refundAmount = priceDifference.multiply(BigDecimal.valueOf(0.75));
            refundPolicy = "75% refund - 14-29 days notice";
        } else if (daysUntilCheckIn >= 7) {
            refundPercentage = 50;
            refundAmount = priceDifference.multiply(BigDecimal.valueOf(0.50));
            refundPolicy = "50% refund - 7-13 days notice";
        } else if (daysUntilCheckIn >= 3) {
            refundPercentage = 25;
            refundAmount = priceDifference.multiply(BigDecimal.valueOf(0.25));
            refundPolicy = "25% refund - 3-6 days notice";
        } else {
            refundPercentage = 0;
            refundAmount = BigDecimal.ZERO;
            refundPolicy = "No refund - Less than 3 days notice";
        }

        logger.info("Refunding ${} ({}%) - {}", refundAmount, refundPercentage, refundPolicy);

        // Process refund if payment exists
        if (paymentRepository.existsByBookingId(booking.getId())) {
            Payment payment = paymentRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new PaymentException("Payment not found"));

            if (payment.getStatus() == Payment.PaymentStatus.COMPLETED &&
                    refundAmount.compareTo(BigDecimal.ZERO) > 0) {

                try {
                    RefundRequestDto refundRequest = RefundRequestDto.builder()
                            .paymentId(payment.getId())
                            .refundAmount(refundAmount)
                            .reason("Booking modification - price decrease | " + refundPolicy)
                            .build();

                    paymentService.refundPayment(refundRequest);
                    logger.info("✅ Partial refund processed: ${}", refundAmount);

                } catch (Exception e) {
                    logger.error("❌ Refund failed: {}", e.getMessage());
                    throw new PaymentException("Failed to process refund: " + e.getMessage());
                }
            }
        }
    }

    // ========================================
    // PRICE INCREASE: ADDITIONAL PAYMENT REQUIRED
    // ========================================

    private void handlePriceIncrease(Booking booking, BigDecimal oldPrice, BigDecimal newPrice) {
        BigDecimal additionalAmount = newPrice.subtract(oldPrice);

        logger.warn("💳 Price increased by ${} - Additional payment required", additionalAmount);

        // ⭐ Set additional payment flag
        booking.setAdditionalPaymentRequired(true);
       //booking.setAdditionalPaymentAmount(additionalAmount);



        // Keep status as CONFIRMED but flag additional payment needed
        logger.warn("⚠️ Booking {} requires additional payment of ${} ",
                booking.getBookingReference(),
                additionalAmount
        );

        // Update existing payment amount
        if (paymentRepository.existsByBookingId(booking.getId())) {
            Payment payment = paymentRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new PaymentException("Payment not found"));

            // Update payment to reflect new total
            payment.setTotalAmount(newPrice);
            payment.setRequiredAdditionalPaymentAmount(additionalAmount);
            payment.setStatus(Payment.PaymentStatus.PARTIALLY_PAID); // Mark as partial until additional payment is made
            paymentRepository.save(payment);
        }

        // TODO: Send notification email to user about additional payment
        // sendAdditionalPaymentNotification(booking, additionalAmount);
    }

    // ========================================
    // TASK 1: Auto-cancel PENDING bookings
    // Runs every minute
    // ========================================

    /**
     * Auto-cancel PENDING bookings older than 3 minutes
     *
     * For PRODUCTION: Change to 3 DAYS
     * Change: .minusMinutes(3) → .minusDays(3)
     */
    @Scheduled(cron = "0 * * * * *") // Every minute
    @Transactional
    public void cancelExpiredPendingBookings() {

        // ⭐ FOR TESTING: 3 minutes
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(1);

        // ⭐ FOR PRODUCTION: Uncomment this line instead
        // LocalDateTime expiryTime = LocalDateTime.now().minusDays(3);

        List<Booking> expiredBookings = bookingRepository
                .findByStatusAndCreatedAtBefore(Booking.BookingStatus.PENDING, expiryTime);

        if (expiredBookings.isEmpty()) {
            return; // No expired bookings
        }

        logger.warn("⏰ Found {} expired PENDING bookings", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                // Cancel the booking
                booking.setStatus(Booking.BookingStatus.CANCELLED);
                booking.setCancelReason("Auto-cancelled: Payment not completed within time limit");
                booking.setCancelledAt(LocalDateTime.now());
                booking.setCancelledBy("SYSTEM");

                bookingRepository.save(booking);

                logger.info("❌ Auto-cancelled PENDING booking: {} (created: {})",
                        booking.getBookingReference(),
                        booking.getCreatedAt());

            } catch (Exception e) {
                logger.error("Failed to auto-cancel booking {}: {}",
                        booking.getId(), e.getMessage());
            }
        }

        logger.info("✅ Auto-cancelled {} PENDING bookings", expiredBookings.size());
    }


    }