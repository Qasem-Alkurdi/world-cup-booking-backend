package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.availability_pricing.availability.AvailabilityService;
import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingService;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRepository;
import com.worldcup.hotelbooking.booking.cancellation.CancellationPolicyService;
import com.worldcup.hotelbooking.booking.cancellation.CancellationResult;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotel.exceptions.HotelNotFoundException;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import com.worldcup.hotelbooking.user.user.AppUserNotFoundException;
import com.worldcup.hotelbooking.user.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Service
@Transactional
public class BookingServiceImp implements BookingService {
    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImp.class);
    private final BookingRepository bookingRepository;
    private final AppUserRepository appUserRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingRoomRepository bookingRoomRepository;
    private final EnhancedPricingService enhancedPricingService;
    private final CancellationPolicyService cancellationPolicyService;
    private final AvailabilityService availabilityService;

    public BookingServiceImp(
            BookingRepository bookingRepository,
            AppUserRepository appUserRepository,
            HotelRepository hotelRepository,
            RoomTypeRepository roomTypeRepository,
            BookingRoomRepository bookingRoomRepository,
            EnhancedPricingService enhancedPricingService,
            CancellationPolicyService cancellationPolicyService,
            AvailabilityService availabilityService) {
        this.bookingRepository = bookingRepository;
        this.appUserRepository = appUserRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingRoomRepository = bookingRoomRepository;
        this.enhancedPricingService = enhancedPricingService;
        this.cancellationPolicyService = cancellationPolicyService;
        this.availabilityService = availabilityService;
    }

    //get
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(Long userId, Booking.BookingStatus status) {
        if (!appUserRepository.existsById(userId)) {
            throw new AppUserNotFoundException("User not found with id: " + userId);
        }
        return bookingRepository.findByAppUser_IdAndStatus(userId, status);
    }

    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(Long userId) {
        if (!appUserRepository.existsById(userId)) {
            throw new AppUserNotFoundException("User not found with id: " + userId);
        }
        return bookingRepository.findByAppUser_Id(userId);
    }

    @Transactional(readOnly = true)
    public List<Booking> getHotelBookings(Long hotelId, Booking.BookingStatus status) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new HotelNotFoundException(hotelId);
        }
        return bookingRepository.findByHotel_IdAndStatus(hotelId, status);
    }

    @Transactional(readOnly = true)
    public List<Booking> getHotelBookings(Long hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new HotelNotFoundException(hotelId);
        }
        return bookingRepository.findByHotel_Id(hotelId);
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
        for (BookingRoom room : booking.getBookingRooms()) {
            if (!availabilityService.checkAvailability(room.getRoomType().getId(), booking.getCheckInDate(), booking.getCheckOutDate(), room.getNumberOfRooms())) {
                throw new IllegalArgumentException("Not enough rooms available for room type: " + room.getRoomType().getName());
            }
        }

        booking.setTotalPrice(calculateTotalPrice(booking));

        booking.getHotel().getBookings().add(booking);
        booking.getAppUser().getBookings().add(booking);
        return bookingRepository.save(booking);
    }

    public BigDecimal calculateTotalPrice(Booking booking) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (BookingRoom room : booking.getBookingRooms()) {
            BigDecimal roomPrice = enhancedPricingService.calculateTotalStayPrice(booking, room.getRoomType().getHotel(), room.getRoomType(), room.getNumberOfRooms());
            totalPrice = totalPrice.add(roomPrice);

        }
        return totalPrice.setScale(2, RoundingMode.HALF_UP);
    }


    @Override
    @Transactional
    public Booking cancelBooking(Long id, String reason) {
        logger.info("Cancelling booking with id: {} for reason: {}", id, reason);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        // CHECK CANCELLATION POLICY
        CancellationResult cancellationResult = cancellationPolicyService.previewCancellation(booking);

        if (!cancellationResult.isCanCancel()) {
            throw new IllegalStateException(cancellationResult.getPolicyMessage());
        }

        // Log refund information
        logger.info("Cancellation approved: Refund ${} ({}%), Fee ${}",
                cancellationResult.getRefundAmount(),
                cancellationResult.getRefundPercentage(),
                cancellationResult.getCancellationFee());

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
    public CancellationResult previewCancellation(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));

        return cancellationPolicyService.previewCancellation(booking);
    }

    @Override
    public Booking confirmBooking(Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
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

    public Booking updateExisting(long id, Booking booking) {
        Booking oldBooking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
        validateCanModify(oldBooking, booking);
        oldBooking.setCheckInDate(booking.getCheckInDate());
        oldBooking.setCheckOutDate(booking.getCheckOutDate());
        oldBooking.setNumberOfGuests(booking.getNumberOfGuests());
        oldBooking.setNumberOfAdults(booking.getNumberOfAdults());
        oldBooking.setNumberOfChildren(booking.getNumberOfChildren());
        oldBooking.setMatchId((booking.getMatchId()));
        oldBooking.setBookingRooms(booking.getBookingRooms());
        BigDecimal oldPrice = oldBooking.getTotalPrice();
        BigDecimal newPrice = calculateTotalPrice(booking);
        if (oldPrice.compareTo(newPrice) > 0)
            newPrice.add(cancellationPolicyService.calculateCancellation(oldBooking).getCancellationFee());

        if (oldBooking.getCheckOutDate().isBefore(oldBooking.getCheckInDate())) {
            throw new IllegalArgumentException("Check-out date cannot be before check-in date");
        }
        if (oldBooking.getBookingRooms() == null || oldBooking.getBookingRooms().isEmpty()) {
            throw new IllegalArgumentException("At least one room must be booked");
        }
        if (!availabilityService.isNumberOfGuestsValid(oldBooking)) {
            throw new IllegalArgumentException("Number of guests exceeds room capacity");
        }
        for (BookingRoom room : oldBooking.getBookingRooms()) {
            if (!availabilityService.checkAvailability(room.getRoomType().getId(), oldBooking.getCheckInDate(), oldBooking.getCheckOutDate(), room.getNumberOfRooms())) {
                throw new IllegalArgumentException("Not enough rooms available for room type: " + room.getRoomType().getName());
            }
        }
        oldBooking.setTotalPrice(newPrice);
        return bookingRepository.save(oldBooking);
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

    public Page<Booking> bookingList(
            Pageable pageable
    ) {
        // start with a no-op specification using a conjunction predicate to avoid null
        Specification<Booking> spec = Specification.where((root, query, criteriaBuilder) -> criteriaBuilder.conjunction());

        Page<Booking> page = bookingRepository.findAll(spec, pageable);


        return page;
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


}