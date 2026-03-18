package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.availability_pricing.availability.AvailabilityServiceImpl;
import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingServiceImpl;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.cancellation.CancellationPolicyServiceImpl;
import com.worldcup.hotelbooking.booking.cancellation.CancellationResponse;
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
        CancellationResponse cancellationResult = cancellationPolicyService.previewCancellation(booking);

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
    public CancellationResponse previewCancellation(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));

        return cancellationPolicyService.previewCancellation(booking);
    }

    /**
     * Preview manager cancellation without actually cancelling.
     * Shows what bonus + refund the guest would receive if the manager cancels now.
     */
    public CancellationResponse previewManagerCancellation(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));

        return cancellationPolicyService.calculateManagerCancellation(booking);
    }

    /**
     * Hotel manager cancels a guest's booking.
     *
     * Differs from guest cancellation in three ways:
     *  1. Refund policy: always 100% base refund + a compensation bonus on top
     *     (bonus increases the closer to check-in, because the disruption is greater).
     *  2. cancelledBy is set to the manager's username (passed from the Security context).
     *  3. The cancel reason is prefixed with "HOTEL_CANCELLED" so reports can distinguish
     *     hotel-side vs guest-side cancellations.
     *
     * @param bookingId       booking to cancel
     * @param reason          reason provided by the manager
     * @param managerUsername username from Spring Security context (never client-supplied)
     */
    @Transactional
    public Booking cancelBookingByManager(Long bookingId, String reason, String managerUsername) {
        logger.info("Manager '{}' cancelling booking id={} — reason: {}", managerUsername, bookingId, reason);

        Booking booking = bookingRepository.findByIdWithRooms(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));

        // Calculates 100% base refund + bonus tier — throws CancellationNotAllowedException
        // if status is CANCELLED, CHECKED_IN, or CHECKED_OUT (same guards as guest cancel)
        CancellationResponse cancellationResult =
                cancellationPolicyService.calculateManagerCancellation(booking);

        BigDecimal baseRefund  = cancellationResult.getRefundAmount();
        BigDecimal bonusAmount = cancellationResult.getBonusAmount();
        BigDecimal totalPayout = cancellationResult.getTotalPayout();

        logger.info("Manager cancellation approved — base refund: ${}, bonus: ${} ({}), total payout: ${}",
                baseRefund, bonusAmount, cancellationResult.getBonusTierDescription(), totalPayout);

        // Process payment refund (base + bonus)
        if (paymentRepository.existsByBookingId(booking.getId())) {
            Payment payment = paymentRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new PaymentException("Payment record not found for booking " + bookingId));

            boolean canRefund = payment.getStatus() == Payment.PaymentStatus.COMPLETED
                    || payment.getStatus() == Payment.PaymentStatus.PARTIALLY_REFUNDED
                    || payment.getStatus() == Payment.PaymentStatus.PARTIALLY_PAID;

            if (canRefund) {
                BigDecimal alreadyRefunded = payment.getRefundAmount() != null
                        ? payment.getRefundAmount() : BigDecimal.ZERO;
                BigDecimal paidAmount = payment.getPaidAmount() != null
                        ? payment.getPaidAmount() : BigDecimal.ZERO;

                // Cap the base refund at what the guest actually paid (safety net),
                // but the bonus is always added on top — it is the hotel's cost
                BigDecimal refundableBase = baseRefund.min(paidAmount.subtract(alreadyRefunded));
                BigDecimal actualPayout   = refundableBase.add(bonusAmount);

                if (actualPayout.compareTo(BigDecimal.ZERO) > 0) {
                    RefundRequestDto refundRequest = RefundRequestDto.builder()
                            .paymentId(payment.getId())
                            .refundAmount(actualPayout)
                            .reason("Hotel-initiated cancellation by manager '" + managerUsername
                                    + "' | " + cancellationResult.getBonusTierDescription()
                                    + " | " + reason)
                            .build();

                    paymentService.refundPayment(refundRequest);
                    logger.info("Refund processed: base ${} + bonus ${} = ${}",
                            refundableBase, bonusAmount, actualPayout);
                }
            }
        }

        // Persist cancellation state
        String fullReason = String.format("HOTEL_CANCELLED | Manager: %s | %s | %s",
                managerUsername, cancellationResult.getBonusTierDescription(), reason);

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelReason(fullReason);
        booking.setCancelledAt(java.time.LocalDateTime.now());
        booking.setCancelledBy(managerUsername);   // manager, not guest

        Booking cancelled = bookingRepository.save(booking);
        logger.info("Booking {} cancelled by manager '{}'. Guest total payout: ${}",
                cancelled.getBookingReference(), managerUsername, totalPayout);

        return cancelled;
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
        return bookingRepository.findByBookingReferenceWithRooms(bookingReference).orElseThrow(() -> new BookingNotFoundException("Booking not found with reference: " + bookingReference));
    }

    @Transactional
    public Booking updateExisting(long id, Booking requestBooking) {
        // 1. Fetch the managed entity with its current rooms
        Booking managedBooking = bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + id));

        // 2. Guard: check the booking is in a state that allows modification
        validateCanModify(managedBooking, requestBooking);

        // 3. Capture the ORIGINAL state in full before applying any changes.
        //    Every field here is the confirmed, fully-paid baseline.
        //    These are passed into handlePriceIncrease to build the snapshot copy.
        BigDecimal oldPrice = managedBooking.getTotalPrice();
        Booking.BookingStatus oldStatus = managedBooking.getStatus();
        LocalDate oldCheckInDate = managedBooking.getCheckInDate();
        LocalDate oldCheckOutDate = managedBooking.getCheckOutDate();
        int oldNumberOfGuests = managedBooking.getNumberOfGuests();
        int oldNumberOfAdults = managedBooking.getNumberOfAdults();
        int oldNumberOfChildren = managedBooking.getNumberOfChildren();
        List<BookingRoom> originalRooms = new java.util.ArrayList<>(managedBooking.getBookingRooms());

        // 4. Apply the requested changes to top-level fields
        managedBooking.setCheckInDate(requestBooking.getCheckInDate());
        managedBooking.setCheckOutDate(requestBooking.getCheckOutDate());
        managedBooking.setNumberOfGuests(requestBooking.getNumberOfGuests());
        managedBooking.setNumberOfAdults(requestBooking.getNumberOfAdults());
        managedBooking.setNumberOfChildren(requestBooking.getNumberOfChildren());

        // 5. Synchronise rooms and recalculate the total price
        BigDecimal newTotal = updateBookingRoomsAndPrice(managedBooking, requestBooking.getBookingRooms());
        managedBooking.setTotalPrice(newTotal);

        // 6. Validate dates, capacity, and availability with the new values
        performBookingValidations(managedBooking);

        // 7. Handle any price change — refund, flag additional payment, or do nothing
        handlePriceChange(managedBooking, oldStatus, oldPrice, newTotal,
                oldCheckInDate, oldCheckOutDate,
                oldNumberOfGuests, oldNumberOfAdults, oldNumberOfChildren,
                originalRooms);

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

        // All modifications are blocked while there is an outstanding payment.
        // The snapshot copy already holds the confirmed baseline. The user has two options:
        //  - Pay the outstanding amount to confirm the update.
        //  - Wait — the system will auto-revert to the original confirmed booking within 24 hours.
        // We do not allow ANY updates (not even price increases) because each new update
        // would require a new snapshot or stacked logic that makes the state hard to reason about.
        if (booking.isAdditionalPaymentRequired()) {
            throw new ModificationNotAllowedException(
                    "This booking has an outstanding payment from a recent update. " +
                            "No further modifications are allowed until the extra amount is paid. " +
                            "To undo the update, simply wait — the system will auto-revert within 24 hours.");
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
                                   BigDecimal newPrice,
                                   LocalDate oldCheckInDate,
                                   LocalDate oldCheckOutDate,
                                   int oldNumberOfGuests,
                                   int oldNumberOfAdults,
                                   int oldNumberOfChildren,
                                   List<BookingRoom> originalRooms) {

        int priceComparison = newPrice.compareTo(oldPrice);

        if (priceComparison == 0) {
            logger.info("✅ Price unchanged: ${}", newPrice);
            return;
        }

        if (oldStatus == Booking.BookingStatus.PENDING) {
            // PENDING: price update only — no copy, no payment touch.
            // The auto-cancel scheduler will clean it up if payment never comes.
            logger.info("📝 PENDING booking - Price updated: ${} → ${}", oldPrice, newPrice);

        } else if (oldStatus == Booking.BookingStatus.CONFIRMED) {

            if (priceComparison < 0) {
                // 💰 Price DECREASED: refund the difference according to policy
                handlePriceDecrease(booking, oldPrice, newPrice);

            } else {
                // 💳 Price INCREASED: create snapshot copy and request additional payment.
                // A second update while additionalPaymentRequired=true is impossible —
                // validateCanModify blocks all modifications in that state.
                handlePriceIncrease(booking, oldPrice, newPrice,
                        oldCheckInDate, oldCheckOutDate,
                        oldNumberOfGuests, oldNumberOfAdults, oldNumberOfChildren,
                        originalRooms);
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

    /**
     * Called when a CONFIRMED booking is updated and the new price is higher.
     *
     * Strategy:
     *  1. Create a full inactive copy of the booking AS IT WAS (dates, guests, rooms, price).
     *     This copy has active=false and snapshotOf pointing to the original booking.
     *  2. Apply the new changes to the original booking (already done by updateExisting).
     *  3. Set a 24h payment deadline on the original booking.
     *  4. Flag additionalPaymentRequired so check-in is blocked.
     *
     * If the user pays within 24h: the copy is deleted (handled in processAdditionalPayment).
     * If they don't: the scheduler restores the original from the copy and deletes the copy.
     *
     * @param booking       the original booking, already updated with the new values
     * @param oldPrice      price before the update (used for payment record diff)
     * @param newPrice      price after the update
     * @param originalRooms the rooms AS THEY WERE before the update — these go into the snapshot copy
     */
    private void handlePriceIncrease(Booking booking,
                                     BigDecimal oldPrice,
                                     BigDecimal newPrice,
                                     LocalDate oldCheckInDate,
                                     LocalDate oldCheckOutDate,
                                     int oldNumberOfGuests,
                                     int oldNumberOfAdults,
                                     int oldNumberOfChildren,
                                     List<BookingRoom> originalRooms) {

        BigDecimal additionalAmount = newPrice.subtract(oldPrice);

        // ── 1. Build the inactive snapshot copy ────────────────────────────
        // IMPORTANT: booking already has the NEW values at this point (updateExisting
        // applied them before calling here). Every field on the snapshot must come
        // from the old* parameters captured BEFORE the update was applied — never
        // from booking.getXxx() for fields that were mutated.
        Booking snapshot = new Booking();
        snapshot.setActive(false);
        snapshot.setSnapshotOf(booking);
        snapshot.setBookingReference(booking.getBookingReference()); // same reference for traceability
        snapshot.setAppUser(booking.getAppUser());
        snapshot.setHotel(booking.getHotel());
        snapshot.setStatus(booking.getStatus());

        // Scalar fields: use the old* params, NOT booking.getXxx() ─────────
        snapshot.setTotalPrice(oldPrice);
        snapshot.setCheckInDate(oldCheckInDate);
        snapshot.setCheckOutDate(oldCheckOutDate);
        snapshot.setNumberOfGuests(oldNumberOfGuests);
        snapshot.setNumberOfAdults(oldNumberOfAdults);
        snapshot.setNumberOfChildren(oldNumberOfChildren);

        // These fields were not mutated by the update — safe to read from booking
        snapshot.setConfirmedAt(booking.getConfirmedAt());
        snapshot.setConfirmationDeadline(booking.getConfirmationDeadline());

        // ── 2. Copy the original rooms into the snapshot ────────────────────
        // originalRooms were captured BEFORE updateBookingRoomsAndPrice cleared
        // the collection — so these are the pre-update rooms.
        for (BookingRoom originalRoom : originalRooms) {
            BookingRoom roomCopy = new BookingRoom();
            roomCopy.setBooking(snapshot);
            roomCopy.setRoomType(originalRoom.getRoomType());
            roomCopy.setNumberOfRooms(originalRoom.getNumberOfRooms());
            roomCopy.setBasePricePerNightPerRoom(originalRoom.getBasePricePerNightPerRoom());
            roomCopy.setTotalPriceWithFees(originalRoom.getTotalPriceWithFees());
            snapshot.getBookingRooms().add(roomCopy);
        }

        bookingRepository.save(snapshot);
        logger.info("📋 Snapshot copy created (id={}) for booking {}",
                snapshot.getId(), booking.getBookingReference());

        // ── 3. Set the 24h deadline on the original booking ────────────────
        booking.setUpdatePaymentDeadline(LocalDateTime.now().plusMinutes(2)); // ⭐ FOR TESTING: 2 minutes
        booking.setAdditionalPaymentRequired(true);

        logger.warn("⚠️ Booking {} requires additional payment of ${} — deadline: {}",
                booking.getBookingReference(), additionalAmount, booking.getUpdatePaymentDeadline());

        // ── 4. Update the payment record to reflect the new total ──────────
        if (paymentRepository.existsByBookingId(booking.getId())) {
            Payment payment = paymentRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new PaymentException("Payment not found"));

            payment.setTotalAmount(newPrice);
            payment.setRequiredAdditionalPaymentAmount(additionalAmount);
            payment.setStatus(Payment.PaymentStatus.PARTIALLY_PAID);
            paymentRepository.save(payment);
        }

        // TODO: Send notification email to user about the additional payment deadline
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

        // ⭐ FOR TESTING: 1 minute
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(1);

        // ⭐ FOR PRODUCTION: Uncomment this line instead
        // LocalDateTime expiryTime = LocalDateTime.now().minusDays(3);

        List<Booking> expiredBookings = bookingRepository
                .findByStatusAndCreatedAtBefore(Booking.BookingStatus.PENDING, expiryTime);

        if (expiredBookings.isEmpty()) {
            return;
        }

        logger.warn("⏰ Found {} expired PENDING bookings", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                booking.setStatus(Booking.BookingStatus.CANCELLED);
                booking.setCancelReason("Auto-cancelled: Payment not completed within time limit");
                booking.setCancelledAt(LocalDateTime.now());
                booking.setCancelledBy("SYSTEM");

                bookingRepository.save(booking);

                logger.info("❌ Auto-cancelled PENDING booking: {} (created: {})",
                        booking.getBookingReference(), booking.getCreatedAt());

            } catch (Exception e) {
                logger.error("Failed to auto-cancel booking {}: {}",
                        booking.getId(), e.getMessage());
            }
        }

        logger.info("✅ Auto-cancelled {} PENDING bookings", expiredBookings.size());
    }

    // ========================================
    // TASK 2: Revert unresolved update payments
    // Runs every hour
    // ========================================

    /**
     * Reverts CONFIRMED bookings where the user updated to a higher-priced option
     * but did not pay the extra amount within 24 hours.
     *
     * How it works:
     *  - Finds all active CONFIRMED bookings with an expired updatePaymentDeadline.
     *  - For each one, finds the inactive snapshot copy (active=false, snapshotOf=booking).
     *  - Copies all fields and rooms from the snapshot back onto the original booking.
     *  - Deletes the snapshot copy.
     *  - Resets the payment record to COMPLETED with the original price.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void revertExpiredUpdatePayments() {

        List<Booking> expiredUpdates = bookingRepository
                .findConfirmedBookingsWithExpiredUpdateDeadline(LocalDateTime.now());

        if (expiredUpdates.isEmpty()) {
            return;
        }

        logger.warn("⏰ Found {} bookings with expired update payment deadline", expiredUpdates.size());

        for (Booking booking : expiredUpdates) {
            try {
                logger.info("Reverting booking {} — deadline was: {}",
                        booking.getBookingReference(), booking.getUpdatePaymentDeadline());

                // ── Find the inactive snapshot copy ──────────────────────────
                Booking snapshot = bookingRepository.findInactiveSnapshotByOriginalReference(booking.getBookingReference())
                        .orElseThrow(() -> new BookingNotFoundException(
                                "Snapshot copy not found for booking " + booking.getBookingReference()));

                // ── Restore scalar fields from snapshot onto original ─────────
                booking.setCheckInDate(snapshot.getCheckInDate());
                booking.setCheckOutDate(snapshot.getCheckOutDate());
                booking.setNumberOfGuests(snapshot.getNumberOfGuests());
                booking.setNumberOfAdults(snapshot.getNumberOfAdults());
                booking.setNumberOfChildren(snapshot.getNumberOfChildren());
                booking.setTotalPrice(snapshot.getTotalPrice());

                // ── Restore rooms: clear current rooms, copy from snapshot ────
                booking.getBookingRooms().clear();
                for (BookingRoom snapshotRoom : snapshot.getBookingRooms()) {
                    BookingRoom restoredRoom = new BookingRoom();
                    restoredRoom.setBooking(booking);
                    restoredRoom.setRoomType(snapshotRoom.getRoomType());
                    restoredRoom.setNumberOfRooms(snapshotRoom.getNumberOfRooms());
                    restoredRoom.setBasePricePerNightPerRoom(snapshotRoom.getBasePricePerNightPerRoom());
                    restoredRoom.setTotalPriceWithFees(snapshotRoom.getTotalPriceWithFees());
                    booking.getBookingRooms().add(restoredRoom);
                }

                // ── Clear the additional-payment flags ───────────────────────
                booking.setAdditionalPaymentRequired(false);
                booking.setUpdatePaymentDeadline(null);
                bookingRepository.save(booking);

                // ── Reset the payment record to the original amount ──────────
                if (paymentRepository.existsByBookingId(booking.getId())) {
                    Payment payment = paymentRepository.findByBookingId(booking.getId())
                            .orElseThrow(() -> new PaymentException("Payment not found"));

                    payment.setTotalAmount(snapshot.getTotalPrice());
                    payment.setRequiredAdditionalPaymentAmount(BigDecimal.ZERO);
                    payment.setStatus(Payment.PaymentStatus.COMPLETED);
                    paymentRepository.save(payment);
                }

                // ── Delete the snapshot copy — it has served its purpose ─────
                bookingRepository.delete(snapshot);

                // TODO: Send notification email to user explaining the revert.

                logger.info("✅ Booking {} reverted to previous state — price restored to ${}",
                        booking.getBookingReference(), booking.getTotalPrice());

            } catch (Exception e) {
                logger.error("❌ Failed to revert booking {}: {}",
                        booking.getId(), e.getMessage());
            }
        }

        logger.info("✅ Reverted {} bookings with expired update payment deadline", expiredUpdates.size());
    }


}