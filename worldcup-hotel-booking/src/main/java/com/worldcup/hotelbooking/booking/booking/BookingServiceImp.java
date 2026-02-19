package com.worldcup.hotelbooking.booking.booking;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public BookingServiceImp(
            BookingRepository bookingRepository,
            AppUserRepository appUserRepository,
            HotelRepository hotelRepository,
            RoomTypeRepository roomTypeRepository,
            BookingRoomRepository bookingRoomRepository,
            EnhancedPricingService enhancedPricingService,
            CancellationPolicyService cancellationPolicyService){
        this.bookingRepository = bookingRepository;
        this.appUserRepository = appUserRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingRoomRepository = bookingRoomRepository;
        this.enhancedPricingService = enhancedPricingService;
        this.cancellationPolicyService=cancellationPolicyService;
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
        if (!isNumberOfGuestsValid(booking)) {
            throw new IllegalArgumentException("Number of guests exceeds room capacity");
        }
        for (BookingRoom room : booking.getBookingRooms()) {
            if (!checkAvailability(room.getRoomType().getId(), booking.getCheckInDate(), booking.getCheckOutDate(), room.getNumberOfRooms())) {
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
            BigDecimal roomPrice = enhancedPricingService.calculateTotalStayPrice(booking,room.getRoomType().getHotel(), room.getRoomType(),room.getNumberOfRooms());
            totalPrice = totalPrice.add(roomPrice);

        }
        return totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public boolean checkAvailability(Long roomTypeId, java.time.LocalDate checkIn, java.time.LocalDate checkOut, int rooms) {
        int bookedRooms = bookingRoomRepository.countBookedRooms(roomTypeId, checkIn, checkOut);
        int availableRooms =
                roomTypeRepository.findById(roomTypeId)
                        .orElseThrow(() -> new IllegalArgumentException("Room type not found with id: " + roomTypeId))
                        .getTotalRooms()
                        - bookedRooms;

        if (availableRooms < rooms) {
            return false;
        }
        return true;
    }

    public boolean isNumberOfGuestsValid(Booking booking) {
        int numberOfValidAdults = 0;
        int numberOfValidChildren = 0;
        for (BookingRoom room : booking.getBookingRooms()) {
            numberOfValidAdults += room.getRoomType().getMaxAdults() * room.getNumberOfRooms();
            numberOfValidChildren += room.getRoomType().getMaxChildren() * room.getNumberOfRooms();
        }
        return booking.getNumberOfAdults() <= numberOfValidAdults && booking.getNumberOfChildren() <= numberOfValidChildren;
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
        booking.setCancelledBy(booking.getAppUser().getName());

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
        if (booking.getStatus()== Booking.BookingStatus.CONFIRMED) {
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

}