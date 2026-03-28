package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.availability_pricing.availability.AvailabilityServiceImpl;
import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingServiceImpl;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.cancellation.CancellationPolicyServiceImpl;
import com.worldcup.hotelbooking.booking.cancellation.CancellationResponse;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.query.hotel.HotelCatalogServiceImpl;
import com.worldcup.hotelbooking.payment.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
            PaymentServiceImpl paymentService){
        this.bookingRepository = bookingRepository;
        this.enhancedPricingService = enhancedPricingService;
        this.cancellationPolicyService = cancellationPolicyService;
        this.availabilityService = availabilityService;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    // ========================================
    // READ OPERATIONS  (populate the cache)
    // ========================================

    /**
     * Cache key = booking id.
     * readOnly = true prevents Spring from flushing the persistence context
     * and avoids any accidental dirty-write on a pure read.
     */
    @Cacheable(value = "bookingById", key = "#id")
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
    }

    /**
     * Cache key = booking reference string.
     * Must be readOnly — there is no mutation here.
     * Without readOnly the class-level @Transactional would apply a write lock unnecessarily.
     */
    @Cacheable(value = "bookingByReference", key = "#bookingReference")
    @Transactional(readOnly = true)
    public Booking findBookingByReference(String bookingReference) {
        return bookingRepository.findByBookingReferenceWithRooms(bookingReference)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with reference: " + bookingReference));
    }

    /**
     * Cache key = userId + pageable (toString gives page/size/sort).
     * Evicted whenever any booking mutation occurs that could affect a guest's history.
     */
    @Cacheable(value = "guestHistory", key = "#userId + '_' + #pageable.toString()")
    @Transactional(readOnly = true)
    public Page<Booking> getGuestHistory(Long userId, Pageable pageable) {
        Specification<Booking> spec =
                Specification.where(BookingSpecifications.hasUser(userId))
                        .and(BookingSpecifications.isPast());
        return bookingRepository.findAll(spec, pageable);
    }

    /**
     * Cache key = hotelId + pageable.
     * Evicted whenever a booking for this hotel changes status, is created, or is cancelled.
     */
    @Cacheable(value = "hotelUpcoming", key = "#hotelId + '_' + #pageable.toString()")
    @Transactional(readOnly = true)
    public Page<Booking> getHotelUpcomingBookings(Long hotelId, Pageable pageable) {
        Specification<Booking> spec =
                Specification.where(BookingSpecifications.hasHotel(hotelId))
                        .and(BookingSpecifications.isUpcoming())
                        .and(BookingSpecifications.hasStatus(Booking.BookingStatus.CONFIRMED));
        return bookingRepository.findAll(spec, pageable);
    }

    // filterBookings is intentionally NOT cached — it accepts too many optional
    // combinations (userId, hotelId, status, date range, price range) to produce
    // a safe and useful cache key. Caching it would risk enormous memory use and
    // near-zero hit rates. Leave it as a pass-through to the database.
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
        Specification<Booking> spec = Specification.where(
                (root, query, cb) -> cb.conjunction());

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

    // ========================================
    // WRITE OPERATIONS  (invalidate the cache)
    // ========================================

    /**
     * Creating a new booking invalidates:
     *   - hotelUpcoming  : the new booking may appear in the hotel's upcoming list once confirmed.
     *   - guestHistory   : less likely immediately, but evict for safety on long-stay bookings.
     *
     * bookingById / bookingByReference do NOT need eviction here because the new booking
     * has never been cached yet — there is nothing stale to remove.
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Caching(evict = {
            @CacheEvict(value = "hotelUpcoming", allEntries = true),
            @CacheEvict(value = "guestHistory",  allEntries = true)
    })
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
            if (!availabilityService.checkAvailability(
                    room.getRoomType().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    room.getNumberOfRooms())) {
                throw new IllegalArgumentException(
                        "Not enough rooms available for room type: " + room.getRoomType().getName());
            }
        }

        booking.setTotalPrice(calculateTotalPrice(booking));
        booking.setConfirmationDeadline(LocalDateTime.now().plusDays(3));

        return bookingRepository.save(booking);
    }

    public BigDecimal calculateTotalPrice(Booking booking) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (BookingRoom room : booking.getBookingRooms()) {
            BigDecimal roomPrice = enhancedPricingService.calculateTotalStayPrice(
                    booking, room.getRoomType().getHotel(), room.getRoomType(), room.getNumberOfRooms());
            totalPrice = totalPrice.add(roomPrice);
            room.setBasePricePerNightPerRoom(room.getRoomType().getBasePrice());
            room.setTotalPriceWithFees(roomPrice);
        }
        return totalPrice.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Cancellation changes the booking's status and may trigger a payment refund.
     * Evict the exact entries for this booking so subsequent reads go to the database.
     * Also evict hotelUpcoming (confirmed booking disappears) and guestHistory.
     */
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookingById",        key = "#id"),
            @CacheEvict(value = "bookingByReference", allEntries = true),
            @CacheEvict(value = "hotelUpcoming",      allEntries = true),
            @CacheEvict(value = "guestHistory",       allEntries = true)
    })
    public Booking cancelBooking(Long id, String reason) {
        logger.info("Cancelling booking with id: {} for reason: {}", id, reason);

        Booking booking = bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        CancellationResponse cancellationResult = cancellationPolicyService.previewCancellation(booking);

        if (!cancellationResult.isCanCancel()) {
            throw new IllegalStateException(cancellationResult.getPolicyMessage());
        }

        logger.info("Cancellation approved: Refund ${} ({}%), Fee ${}",
                cancellationResult.getRefundAmount(),
                cancellationResult.getRefundPercentage(),
                cancellationResult.getCancellationFee());

        if (paymentRepository.existsByBookingId(booking.getId())) {
            Payment payment = paymentRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new PaymentException("Payment not found"));

            boolean canRefund = payment.getStatus() == Payment.PaymentStatus.COMPLETED
                    || payment.getStatus() == Payment.PaymentStatus.PARTIALLY_REFUNDED;

            if (canRefund && cancellationResult.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {

                BigDecimal alreadyRefunded = payment.getRefundAmount() != null
                        ? payment.getRefundAmount() : BigDecimal.ZERO;
                BigDecimal paidAmount = payment.getPaidAmount() != null
                        ? payment.getPaidAmount() : BigDecimal.ZERO;
                BigDecimal maxRefundable = paidAmount.subtract(alreadyRefunded);
                BigDecimal cancellationRefund = cancellationResult.getRefundAmount().min(maxRefundable);

                if (cancellationRefund.compareTo(BigDecimal.ZERO) > 0) {
                    RefundRequestDto refundRequest = RefundRequestDto.builder()
                            .paymentId(payment.getId())
                            .refundAmount(cancellationRefund)
                            .reason(reason + " | " + cancellationResult.getPolicyMessage())
                            .build();
                    paymentService.refundPayment(refundRequest);
                }
            }
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelReason(reason + " | " + cancellationResult.getPolicyMessage());
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(booking.getAppUser().getUsername());

        Booking cancelled = bookingRepository.save(booking);
        logger.info("Booking {} cancelled successfully - Refund: ${}",
                cancelled.getBookingReference(), cancellationResult.getRefundAmount());

        return cancelled;
    }

    /**
     * Manager cancellation — same eviction strategy as guest cancellation.
     * bookingByReference uses allEntries = true because we only have the id here,
     * not the reference string, so we cannot target the exact key cheaply.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookingById",        key = "#bookingId"),
            @CacheEvict(value = "bookingByReference", allEntries = true),
            @CacheEvict(value = "hotelUpcoming",      allEntries = true),
            @CacheEvict(value = "guestHistory",       allEntries = true)
    })
    public Booking cancelBookingByManager(Long bookingId, String reason, String managerUsername) {
        logger.info("Manager '{}' cancelling booking id={} — reason: {}", managerUsername, bookingId, reason);

        Booking booking = bookingRepository.findByIdWithRooms(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));

        CancellationResponse cancellationResult =
                cancellationPolicyService.calculateManagerCancellation(booking);

        BigDecimal baseRefund  = cancellationResult.getRefundAmount();
        BigDecimal bonusAmount = cancellationResult.getBonusAmount();
        BigDecimal totalPayout = cancellationResult.getTotalPayout();

        logger.info("Manager cancellation approved — base refund: ${}, bonus: ${} ({}), total payout: ${}",
                baseRefund, bonusAmount, cancellationResult.getBonusTierDescription(), totalPayout);

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

        String fullReason = String.format("HOTEL_CANCELLED | Manager: %s | %s | %s",
                managerUsername, cancellationResult.getBonusTierDescription(), reason);

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelReason(fullReason);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(managerUsername);

        Booking cancelled = bookingRepository.save(booking);
        logger.info("Booking {} cancelled by manager '{}'. Guest total payout: ${}",
                cancelled.getBookingReference(), managerUsername, totalPayout);

        return cancelled;
    }

    /**
     * Confirming a booking changes its status from PENDING → CONFIRMED.
     * It must now appear in hotelUpcoming, and both single-booking caches become stale.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "bookingById",        key = "#id"),
            @CacheEvict(value = "bookingByReference", allEntries = true),
            @CacheEvict(value = "hotelUpcoming",      allEntries = true),
            @CacheEvict(value = "guestHistory",       allEntries = true)
    })
    public Booking confirmBooking(Long id) {
        Booking booking = bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking is already confirmed");
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled booking cannot be confirmed");
        }

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    /**
     * Updating a booking changes dates, guests, rooms, and potentially price.
     * Evict by id; use allEntries for the reference cache since we have no reference string here.
     * hotelUpcoming and guestHistory may both change due to date or status implications.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookingById",        key = "#id"),
            @CacheEvict(value = "bookingByReference", allEntries = true),
            @CacheEvict(value = "hotelUpcoming",      allEntries = true),
            @CacheEvict(value = "guestHistory",       allEntries = true)
    })
    public Booking updateExisting(long id, Booking requestBooking) {
        Booking managedBooking = bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + id));

        validateCanModify(managedBooking, requestBooking);

        BigDecimal oldPrice           = managedBooking.getTotalPrice();
        Booking.BookingStatus oldStatus = managedBooking.getStatus();
        LocalDate oldCheckInDate      = managedBooking.getCheckInDate();
        LocalDate oldCheckOutDate     = managedBooking.getCheckOutDate();
        int oldNumberOfGuests         = managedBooking.getNumberOfGuests();
        int oldNumberOfAdults         = managedBooking.getNumberOfAdults();
        int oldNumberOfChildren       = managedBooking.getNumberOfChildren();
        List<BookingRoom> originalRooms = new java.util.ArrayList<>(managedBooking.getBookingRooms());

        managedBooking.setCheckInDate(requestBooking.getCheckInDate());
        managedBooking.setCheckOutDate(requestBooking.getCheckOutDate());
        managedBooking.setNumberOfGuests(requestBooking.getNumberOfGuests());
        managedBooking.setNumberOfAdults(requestBooking.getNumberOfAdults());
        managedBooking.setNumberOfChildren(requestBooking.getNumberOfChildren());

        BigDecimal newTotal = updateBookingRoomsAndPrice(managedBooking, requestBooking.getBookingRooms());
        managedBooking.setTotalPrice(newTotal);

        performBookingValidations(managedBooking);

        handlePriceChange(managedBooking, oldStatus, oldPrice, newTotal,
                oldCheckInDate, oldCheckOutDate,
                oldNumberOfGuests, oldNumberOfAdults, oldNumberOfChildren,
                originalRooms);

        return bookingRepository.save(managedBooking);
    }

    /**
     * Check-in changes status to CHECKED_IN.
     * The booking disappears from hotelUpcoming (which only shows CONFIRMED) once it transitions.
     */
    @Caching(evict = {
            @CacheEvict(value = "bookingById",        key = "#id"),
            @CacheEvict(value = "bookingByReference", allEntries = true),
            @CacheEvict(value = "hotelUpcoming",      allEntries = true),
            @CacheEvict(value = "guestHistory",       allEntries = true)
    })
    public Booking checkInBooking(Long id) {
        Booking booking = bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found"));

        if (!booking.canCheckIn()) {
            if (booking.isAdditionalPaymentRequired()) {
                throw new IllegalStateException(
                        "Cannot check in - Additional payment is required. Please complete payment before check-in.");
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

    /**
     * Check-out changes status to CHECKED_OUT.
     * The completed stay should now appear in guestHistory.
     */
    @Caching(evict = {
            @CacheEvict(value = "bookingById",        key = "#id"),
            @CacheEvict(value = "bookingByReference", allEntries = true),
            @CacheEvict(value = "hotelUpcoming",      allEntries = true),
            @CacheEvict(value = "guestHistory",       allEntries = true)
    })
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
    // PREVIEW HELPERS  (no mutation, no cache)
    // ========================================

    public CancellationResponse previewCancellation(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));
        return cancellationPolicyService.previewCancellation(booking);
    }

    public CancellationResponse previewManagerCancellation(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));
        return cancellationPolicyService.calculateManagerCancellation(booking);
    }

    public void addBookingRoom(BookingRoom bookingRoom) {
        Booking booking = bookingRoom.getBooking();
        booking.getBookingRooms().add(bookingRoom);
        bookingRoom.setBooking(booking);
    }

    // ========================================
    // SCHEDULED TASKS  (bulk mutations — evict everything)
    // ========================================

    /**
     * Auto-cancel PENDING bookings older than 3 minutes (testing) / 3 days (production).
     *
     * Evicts ALL entries from every booking-related cache because:
     *   - Multiple bookings across multiple users/hotels are mutated in one sweep.
     *   - Targeted eviction by key is impossible without iterating every booking id.
     *
     * For PRODUCTION: Change .minusMinutes(1) → .minusDays(3)
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookingById",        allEntries = true),
            @CacheEvict(value = "bookingByReference", allEntries = true),
            @CacheEvict(value = "hotelUpcoming",      allEntries = true),
            @CacheEvict(value = "guestHistory",       allEntries = true)
    })
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
                logger.error("Failed to auto-cancel booking {}: {}", booking.getId(), e.getMessage());
            }
        }

        logger.info("✅ Auto-cancelled {} PENDING bookings", expiredBookings.size());
    }

    /**
     * Reverts CONFIRMED bookings where additional payment was not completed within the deadline.
     * Same broad eviction rationale as cancelExpiredPendingBookings.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "bookingById",        allEntries = true),
            @CacheEvict(value = "bookingByReference", allEntries = true),
            @CacheEvict(value = "hotelUpcoming",      allEntries = true),
            @CacheEvict(value = "guestHistory",       allEntries = true)
    })
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

                Booking snapshot = bookingRepository
                        .findInactiveSnapshotByOriginalReference(booking.getBookingReference())
                        .orElseThrow(() -> new BookingNotFoundException(
                                "Snapshot copy not found for booking " + booking.getBookingReference()));

                booking.setCheckInDate(snapshot.getCheckInDate());
                booking.setCheckOutDate(snapshot.getCheckOutDate());
                booking.setNumberOfGuests(snapshot.getNumberOfGuests());
                booking.setNumberOfAdults(snapshot.getNumberOfAdults());
                booking.setNumberOfChildren(snapshot.getNumberOfChildren());
                booking.setTotalPrice(snapshot.getTotalPrice());

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

                booking.setAdditionalPaymentRequired(false);
                booking.setUpdatePaymentDeadline(null);
                bookingRepository.save(booking);

                if (paymentRepository.existsByBookingId(booking.getId())) {
                    Payment payment = paymentRepository.findByBookingId(booking.getId())
                            .orElseThrow(() -> new PaymentException("Payment not found"));
                    payment.setTotalAmount(snapshot.getTotalPrice());
                    payment.setRequiredAdditionalPaymentAmount(BigDecimal.ZERO);
                    payment.setStatus(Payment.PaymentStatus.COMPLETED);
                    paymentRepository.save(payment);
                }

                bookingRepository.delete(snapshot);

                logger.info("✅ Booking {} reverted to previous state — price restored to ${}",
                        booking.getBookingReference(), booking.getTotalPrice());

            } catch (Exception e) {
                logger.error("❌ Failed to revert booking {}: {}", booking.getId(), e.getMessage());
            }
        }

        logger.info("✅ Reverted {} bookings with expired update payment deadline", expiredUpdates.size());
    }

    // ========================================
    // PRIVATE HELPERS
    // ========================================

    private BigDecimal updateBookingRoomsAndPrice(Booking managed, List<BookingRoom> requestedRooms) {
        managed.getBookingRooms().clear();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (BookingRoom room : requestedRooms) {
            BigDecimal roomPrice = enhancedPricingService.calculateTotalStayPrice(
                    managed, room.getRoomType().getHotel(), room.getRoomType(), room.getNumberOfRooms());
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
        for (BookingRoom room : booking.getBookingRooms()) {
            boolean available = availabilityService.checkAvailability(
                    room.getRoomType().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    room.getNumberOfRooms()
            );
            if (!available) {
                throw new IllegalArgumentException(
                        "Room type " + room.getRoomType().getName() + " is full for these dates.");
            }
        }
    }

    private void validateCanModify(Booking booking, Booking request) {
        if(booking.getAppUser().getId() != request.getAppUser().getId())
            throw new ModificationNotAllowedException("Cannot modify another user's booking");
        if (booking.getHotel().getId() != request.getHotel().getId())
            throw new ModificationNotAllowedException("Cannot modify the hotel");

        if (booking.getStatus() == Booking.BookingStatus.CHECKED_IN)
            throw new ModificationNotAllowedException("Cannot modify after check-in. Contact reception.");

        if (booking.getStatus() == Booking.BookingStatus.CHECKED_OUT)
            throw new ModificationNotAllowedException("Cannot modify completed booking");

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED)
            throw new ModificationNotAllowedException("Cannot modify cancelled booking. Create new booking.");

        if (booking.getCheckInDate().isBefore(LocalDate.now()))
            throw new ModificationNotAllowedException("Cannot modify after check-in date passed");

        if (booking.isAdditionalPaymentRequired())
            throw new ModificationNotAllowedException(
                    "This booking has an outstanding payment from a recent update. " +
                            "No further modifications are allowed until the extra amount is paid. " +
                            "To undo the update, simply wait — the system will auto-revert within 24 hours.");
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
            logger.info("📝 PENDING booking - Price updated: ${} → ${}", oldPrice, newPrice);

        } else if (oldStatus == Booking.BookingStatus.CONFIRMED) {
            if (priceComparison < 0) {
                handlePriceDecrease(booking, oldPrice, newPrice);
            } else {
                handlePriceIncrease(booking, oldPrice, newPrice,
                        oldCheckInDate, oldCheckOutDate,
                        oldNumberOfGuests, oldNumberOfAdults, oldNumberOfChildren,
                        originalRooms);
            }
        }
    }

    private void handlePriceDecrease(Booking booking, BigDecimal oldPrice, BigDecimal newPrice) {
        BigDecimal priceDifference = oldPrice.subtract(newPrice);
        logger.info("💰 Price decreased by ${}", priceDifference);

        long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), booking.getCheckInDate());

        BigDecimal refundAmount;
        int refundPercentage;
        String refundPolicy;

        if (daysUntilCheckIn >= 30) {
            refundPercentage = 100;
            refundAmount     = priceDifference;
            refundPolicy     = "Full refund - 30+ days notice";
        } else if (daysUntilCheckIn >= 14) {
            refundPercentage = 75;
            refundAmount     = priceDifference.multiply(BigDecimal.valueOf(0.75));
            refundPolicy     = "75% refund - 14-29 days notice";
        } else if (daysUntilCheckIn >= 7) {
            refundPercentage = 50;
            refundAmount     = priceDifference.multiply(BigDecimal.valueOf(0.50));
            refundPolicy     = "50% refund - 7-13 days notice";
        } else if (daysUntilCheckIn >= 3) {
            refundPercentage = 25;
            refundAmount     = priceDifference.multiply(BigDecimal.valueOf(0.25));
            refundPolicy     = "25% refund - 3-6 days notice";
        } else {
            refundPercentage = 0;
            refundAmount     = BigDecimal.ZERO;
            refundPolicy     = "No refund - Less than 3 days notice";
        }

        logger.info("Refunding ${} ({}%) - {}", refundAmount, refundPercentage, refundPolicy);

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

        Booking snapshot = new Booking();
        snapshot.setActive(false);
        snapshot.setSnapshotOf(booking);
        snapshot.setBookingReference(booking.getBookingReference());
        snapshot.setAppUser(booking.getAppUser());
        snapshot.setHotel(booking.getHotel());
        snapshot.setStatus(booking.getStatus());
        snapshot.setTotalPrice(oldPrice);
        snapshot.setCheckInDate(oldCheckInDate);
        snapshot.setCheckOutDate(oldCheckOutDate);
        snapshot.setNumberOfGuests(oldNumberOfGuests);
        snapshot.setNumberOfAdults(oldNumberOfAdults);
        snapshot.setNumberOfChildren(oldNumberOfChildren);
        snapshot.setConfirmedAt(booking.getConfirmedAt());
        snapshot.setConfirmationDeadline(booking.getConfirmationDeadline());

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

        booking.setUpdatePaymentDeadline(LocalDateTime.now().plusMinutes(2)); // ⭐ FOR TESTING: 2 minutes
        booking.setAdditionalPaymentRequired(true);

        logger.warn("⚠️ Booking {} requires additional payment of ${} — deadline: {}",
                booking.getBookingReference(), additionalAmount, booking.getUpdatePaymentDeadline());

        if (paymentRepository.existsByBookingId(booking.getId())) {
            Payment payment = paymentRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new PaymentException("Payment not found"));
            payment.setTotalAmount(newPrice);
            payment.setRequiredAdditionalPaymentAmount(additionalAmount);
            payment.setStatus(Payment.PaymentStatus.PARTIALLY_PAID);
            paymentRepository.save(payment);
        }
    }

}