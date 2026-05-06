package com.worldcup.hotelbooking.reservation.booking;

import com.worldcup.hotelbooking.availability_pricing.availability.AvailabilityServiceImpl;
import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingServiceImpl;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.payment.Payment;
import com.worldcup.hotelbooking.payment.PaymentRepository;
import com.worldcup.hotelbooking.payment.PaymentServiceImpl;
import com.worldcup.hotelbooking.payment.RefundRequestDto;
import com.worldcup.hotelbooking.reservation.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.reservation.cancellation.CancellationPolicyServiceImpl;
import com.worldcup.hotelbooking.reservation.cancellation.CancellationResponse;
import com.worldcup.hotelbooking.user.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EnhancedPricingServiceImpl enhancedPricingService;

    @Mock
    private CancellationPolicyServiceImpl cancellationPolicyService;

    @Mock
    private AvailabilityServiceImpl availabilityService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentServiceImpl paymentService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Booking booking;
    private BookingRoom bookingRoom;
    private RoomType roomType;
    private Hotel hotel;
    private AppUser user;

    @BeforeEach
    void setUp() {
        hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Test Hotel");

        roomType = new RoomType();
        roomType.setId(10L);
        roomType.setName("Deluxe");
        roomType.setHotel(hotel);
        roomType.setBasePrice(BigDecimal.valueOf(120));

        user = new AppUser();
        user.setId(1L);
        user.setUsername("test-user");

        bookingRoom = new BookingRoom();
        bookingRoom.setRoomType(roomType);
        bookingRoom.setNumberOfRooms(1);

        booking = new Booking();
        booking.setId(1L);
        booking.setBookingReference("WC-REF-001");
        booking.setAppUser(user);
        booking.setHotel(hotel);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        booking.setCheckOutDate(LocalDate.now().plusDays(12));
        booking.setNumberOfAdults(2);
        booking.setNumberOfChildren(1);
        booking.setNumberOfGuests(3);
        booking.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setTotalPrice(BigDecimal.valueOf(240));
        booking.setCreatedAt(LocalDateTime.now().minusHours(1));
        booking.setAdditionalPaymentRequired(false);
    }

    @Test
    void getBookingById_shouldReturnBooking_whenExists() {
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        Booking result = bookingService.getBookingById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(bookingRepository).findByIdWithRooms(1L);
    }

    @Test
    void getBookingById_shouldThrow_whenNotFound() {
        when(bookingRepository.findByIdWithRooms(99L)).thenReturn(Optional.empty());

        BookingNotFoundException ex = assertThrows(
                BookingNotFoundException.class,
                () -> bookingService.getBookingById(99L)
        );

        assertTrue(ex.getMessage().contains("99"));
        verify(bookingRepository).findByIdWithRooms(99L);
    }

    @Test
    void createBooking_shouldSucceed_whenValidData() {
        when(availabilityService.isNumberOfGuestsValid(booking)).thenReturn(true);
        when(availabilityService.checkAvailability(eq(10L), any(LocalDate.class), any(LocalDate.class), eq(1)))
                .thenReturn(true);
        when(enhancedPricingService.calculateTotalStayPrice(any(Booking.class), any(Hotel.class), any(RoomType.class), eq(1)))
                .thenReturn(BigDecimal.valueOf(240));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createBooking(booking);

        assertEquals(Booking.BookingStatus.PENDING, result.getStatus());
        assertEquals(new BigDecimal("240.00"), result.getTotalPrice());
        assertNotNull(result.getConfirmationDeadline());
        verify(bookingRepository).save(booking);
    }

    @Test
    void createBooking_shouldThrow_whenCheckoutBeforeCheckin() {
        booking.setCheckInDate(LocalDate.now().plusDays(5));
        booking.setCheckOutDate(LocalDate.now().plusDays(4));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(booking)
        );

        assertTrue(ex.getMessage().contains("Check-out"));
        verifyNoInteractions(availabilityService, enhancedPricingService);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldThrow_whenNoRooms() {
        booking.setBookingRooms(new ArrayList<>());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(booking)
        );

        assertTrue(ex.getMessage().contains("At least one room"));
        verifyNoInteractions(availabilityService, enhancedPricingService);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldThrow_whenGuestCountMismatch() {
        booking.setNumberOfGuests(4); // adults + children = 3
        when(availabilityService.isNumberOfGuestsValid(booking)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(booking)
        );

        assertTrue(ex.getMessage().contains("sum of adults and children"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldThrow_whenGuestsExceedCapacity() {
        when(availabilityService.isNumberOfGuestsValid(booking)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(booking)
        );

        assertTrue(ex.getMessage().contains("exceeds room capacity"));
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(enhancedPricingService);
    }

    @Test
    void createBooking_shouldThrow_whenRoomNotAvailable() {
        when(availabilityService.isNumberOfGuestsValid(booking)).thenReturn(true);
        when(availabilityService.checkAvailability(eq(10L), any(LocalDate.class), any(LocalDate.class), eq(1)))
                .thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> bookingService.createBooking(booking)
        );

        assertTrue(ex.getMessage().contains("Not enough rooms available"));
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(enhancedPricingService);
    }

    @Test
    void calculateTotalPrice_shouldSumAllRoomPrices() {
        BookingRoom room1 = new BookingRoom();
        room1.setRoomType(roomType);
        room1.setNumberOfRooms(1);

        RoomType secondType = new RoomType();
        secondType.setId(11L);
        secondType.setName("Standard");
        secondType.setHotel(hotel);
        secondType.setBasePrice(BigDecimal.valueOf(80));

        BookingRoom room2 = new BookingRoom();
        room2.setRoomType(secondType);
        room2.setNumberOfRooms(2);

        booking.setBookingRooms(new ArrayList<>(List.of(room1, room2)));

        when(enhancedPricingService.calculateTotalStayPrice(any(Booking.class), any(Hotel.class), eq(roomType), eq(1)))
                .thenReturn(BigDecimal.valueOf(200));
        when(enhancedPricingService.calculateTotalStayPrice(any(Booking.class), any(Hotel.class), eq(secondType), eq(2)))
                .thenReturn(BigDecimal.valueOf(320));

        BigDecimal result = bookingService.calculateTotalPrice(booking);

        assertEquals(new BigDecimal("520.00"), result);
        assertEquals(BigDecimal.valueOf(200), room1.getTotalPriceWithFees());
        assertEquals(BigDecimal.valueOf(320), room2.getTotalPriceWithFees());
        assertEquals(BigDecimal.valueOf(120), room1.getBasePricePerNightPerRoom());
        assertEquals(BigDecimal.valueOf(80), room2.getBasePricePerNightPerRoom());
    }

    @Test
    void cancelBooking_shouldCancelAndRefund_whenAllowedAndPaymentCompleted() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        CancellationResponse cancellation = CancellationResponse.builder()
                .canCancel(true)
                .refundAmount(BigDecimal.valueOf(240))
                .refundPercentage(100)
                .cancellationFee(BigDecimal.ZERO)
                .policyMessage("Full refund - 30+ days notice")
                .build();

        Payment payment = new Payment();
        payment.setId(55L);
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAmount(BigDecimal.valueOf(240));
        payment.setRefundAmount(BigDecimal.ZERO);

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(cancellationPolicyService.previewCancellation(booking)).thenReturn(cancellation);
        when(paymentRepository.existsByBookingId(1L)).thenReturn(true);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.cancelBooking(1L, "User request");

        assertEquals(Booking.BookingStatus.CANCELLED, result.getStatus());
        assertEquals("test-user", result.getCancelledBy());
        assertNotNull(result.getCancelledAt());
        assertTrue(result.getCancelReason().contains("User request"));

        ArgumentCaptor<RefundRequestDto> captor = ArgumentCaptor.forClass(RefundRequestDto.class);
        verify(paymentService).refundPayment(captor.capture());
        assertEquals(BigDecimal.valueOf(240), captor.getValue().getRefundAmount());
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBooking_shouldThrow_whenPolicyDisallowsCancellation() {
        CancellationResponse cancellation = CancellationResponse.builder()
                .canCancel(false)
                .refundAmount(BigDecimal.ZERO)
                .cancellationFee(BigDecimal.valueOf(100))
                .policyMessage("Cannot cancel within 24 hours")
                .build();

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(cancellationPolicyService.previewCancellation(booking)).thenReturn(cancellation);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> bookingService.cancelBooking(1L, "Late cancellation")
        );

        assertTrue(ex.getMessage().contains("Cannot cancel"));
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(paymentService);
    }

    @Test
    void cancelBooking_shouldThrow_whenBookingNotFound() {
        when(bookingRepository.findByIdWithRooms(999L)).thenReturn(Optional.empty());

        assertThrows(
                BookingNotFoundException.class,
                () -> bookingService.cancelBooking(999L, "reason")
        );

        verifyNoInteractions(cancellationPolicyService, paymentRepository, paymentService);
    }


    @Test
    void cancelBookingByManager_shouldCancelAndRefund_whenPaymentExists() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        CancellationResponse response = CancellationResponse.builder()
                .canCancel(true)
                .refundAmount(BigDecimal.valueOf(200))
                .bonusAmount(BigDecimal.valueOf(50))
                .bonusTierDescription("Gold Tier Bonus")
                .build();

        Payment payment = new Payment();
        payment.setId(42L);
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAmount(BigDecimal.valueOf(240));
        payment.setRefundAmount(BigDecimal.ZERO);

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(cancellationPolicyService.calculateManagerCancellation(booking)).thenReturn(response);
        when(paymentRepository.existsByBookingId(1L)).thenReturn(true);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.cancelBookingByManager(1L, "Double booking error", "manager_john");

        assertEquals(Booking.BookingStatus.CANCELLED, result.getStatus());
        assertEquals("manager_john", result.getCancelledBy());
        assertTrue(result.getCancelReason().contains("HOTEL_CANCELLED"));

        ArgumentCaptor<RefundRequestDto> captor = ArgumentCaptor.forClass(RefundRequestDto.class);
        verify(paymentService).refundPayment(captor.capture());
        assertEquals(BigDecimal.valueOf(250), captor.getValue().getRefundAmount());
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBookingByManager_shouldNotRefund_whenNoPaymentRecord() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        CancellationResponse response = CancellationResponse.builder()
                .canCancel(true)
                .refundAmount(BigDecimal.valueOf(200))
                .bonusAmount(BigDecimal.valueOf(50))
                .bonusTierDescription("Tier Bonus")
                .build();

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(cancellationPolicyService.calculateManagerCancellation(booking)).thenReturn(response);
        when(paymentRepository.existsByBookingId(1L)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.cancelBookingByManager(1L, "reason", "mgr");

        assertEquals(Booking.BookingStatus.CANCELLED, result.getStatus());
        verify(paymentService, never()).refundPayment(any());
        verify(bookingRepository).save(booking);
    }

    @Test
    void confirmBooking_shouldSucceed_whenPending() {
        booking.setStatus(Booking.BookingStatus.PENDING);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.confirmBooking(1L);

        assertEquals(Booking.BookingStatus.CONFIRMED, result.getStatus());
        assertNotNull(result.getConfirmedAt());
        verify(bookingRepository).save(booking);
    }

    @Test
    void confirmBooking_shouldThrow_whenAlreadyConfirmed() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> bookingService.confirmBooking(1L)
        );

        assertTrue(ex.getMessage().contains("already confirmed"));
    }

    @Test
    void confirmBooking_shouldThrow_whenCancelled() {
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> bookingService.confirmBooking(1L)
        );

        assertTrue(ex.getMessage().contains("cannot be confirmed"));
    }

    @Test
    void updateExisting_shouldSucceed_whenPendingAndValid() {
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setTotalPrice(BigDecimal.valueOf(200));
        booking.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));
        booking.setAppUser(user);

        Booking request = new Booking();
        request.setAppUser(user);
        request.setHotel(hotel);
        request.setCheckInDate(LocalDate.now().plusDays(11));
        request.setCheckOutDate(LocalDate.now().plusDays(13));
        request.setNumberOfAdults(2);
        request.setNumberOfChildren(1);
        request.setNumberOfGuests(3);

        BookingRoom reqRoom = new BookingRoom();
        reqRoom.setRoomType(roomType);
        reqRoom.setNumberOfRooms(1);
        request.setBookingRooms(new ArrayList<>(List.of(reqRoom)));

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(enhancedPricingService.calculateTotalStayPrice(any(Booking.class), any(Hotel.class), any(RoomType.class), eq(1)))
                .thenReturn(BigDecimal.valueOf(250));
        when(availabilityService.isNumberOfGuestsValid(any(Booking.class))).thenReturn(true);
        when(availabilityService.checkAvailability(eq(10L), any(LocalDate.class), any(LocalDate.class), eq(1)))
                .thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.updateExisting(1L, request);

        assertEquals(new BigDecimal("250.00"), result.getTotalPrice());
        assertEquals(request.getCheckInDate(), result.getCheckInDate());
        assertEquals(request.getCheckOutDate(), result.getCheckOutDate());
        verify(bookingRepository).save(booking);
    }

    @Test
    void updateExisting_shouldThrow_whenAdditionalPaymentRequired() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setAdditionalPaymentRequired(true);
        booking.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));
        booking.setAppUser(user);

        Booking request = new Booking();
        request.setAppUser(user);
        request.setHotel(hotel);
        request.setCheckInDate(booking.getCheckInDate());
        request.setCheckOutDate(booking.getCheckOutDate());
        request.setNumberOfAdults(booking.getNumberOfAdults());
        request.setNumberOfChildren(booking.getNumberOfChildren());
        request.setNumberOfGuests(booking.getNumberOfGuests());
        request.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        ModificationNotAllowedException ex = assertThrows(
                ModificationNotAllowedException.class,
                () -> bookingService.updateExisting(1L, request)
        );

        assertTrue(ex.getMessage().contains("outstanding payment"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateExisting_shouldThrow_whenUserMismatch() {
        AppUser anotherUser = new AppUser();
        anotherUser.setId(99L);

        booking.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));
        booking.setAppUser(user);

        Booking request = new Booking();
        request.setAppUser(anotherUser);
        request.setHotel(hotel);
        request.setCheckInDate(booking.getCheckInDate());
        request.setCheckOutDate(booking.getCheckOutDate());
        request.setNumberOfAdults(booking.getNumberOfAdults());
        request.setNumberOfChildren(booking.getNumberOfChildren());
        request.setNumberOfGuests(booking.getNumberOfGuests());
        request.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        assertThrows(ModificationNotAllowedException.class,
                () -> bookingService.updateExisting(1L, request));
    }

    @Test
    void updateExisting_shouldThrow_whenHotelChanges() {
        Hotel anotherHotel = new Hotel();
        anotherHotel.setId(999L);

        booking.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));
        booking.setAppUser(user);

        Booking request = new Booking();
        request.setAppUser(user);
        request.setHotel(anotherHotel);
        request.setCheckInDate(booking.getCheckInDate());
        request.setCheckOutDate(booking.getCheckOutDate());
        request.setNumberOfAdults(booking.getNumberOfAdults());
        request.setNumberOfChildren(booking.getNumberOfChildren());
        request.setNumberOfGuests(booking.getNumberOfGuests());
        request.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        ModificationNotAllowedException ex = assertThrows(
                ModificationNotAllowedException.class,
                () -> bookingService.updateExisting(1L, request)
        );

        assertTrue(ex.getMessage().contains("Cannot modify the hotel"));
    }

    @Test
    void updateExisting_shouldThrow_whenCheckedIn() {
        booking.setStatus(Booking.BookingStatus.CHECKED_IN);
        booking.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));
        booking.setAppUser(user);

        Booking request = new Booking();
        request.setAppUser(user);
        request.setHotel(hotel);
        request.setCheckInDate(booking.getCheckInDate());
        request.setCheckOutDate(booking.getCheckOutDate());
        request.setNumberOfAdults(booking.getNumberOfAdults());
        request.setNumberOfChildren(booking.getNumberOfChildren());
        request.setNumberOfGuests(booking.getNumberOfGuests());
        request.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        assertThrows(ModificationNotAllowedException.class,
                () -> bookingService.updateExisting(1L, request));
    }

    @Test
    void updateExisting_shouldCreateSnapshotAndRequirePayment_whenConfirmedPriceIncreases() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setTotalPrice(BigDecimal.valueOf(200));
        booking.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));
        booking.setAppUser(user);
        booking.setConfirmedAt(LocalDateTime.now().minusDays(1));
        booking.setConfirmationDeadline(LocalDateTime.now().plusDays(1));

        Booking request = new Booking();
        request.setAppUser(user);
        request.setHotel(hotel);
        request.setCheckInDate(booking.getCheckInDate());
        request.setCheckOutDate(booking.getCheckOutDate());
        request.setNumberOfAdults(2);
        request.setNumberOfChildren(1);
        request.setNumberOfGuests(3);

        BookingRoom reqRoom = new BookingRoom();
        reqRoom.setRoomType(roomType);
        reqRoom.setNumberOfRooms(2);
        request.setBookingRooms(new ArrayList<>(List.of(reqRoom)));

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(enhancedPricingService.calculateTotalStayPrice(any(Booking.class), any(Hotel.class), any(RoomType.class), eq(2)))
                .thenReturn(BigDecimal.valueOf(300));
        when(availabilityService.isNumberOfGuestsValid(any(Booking.class))).thenReturn(true);
        when(availabilityService.checkAvailability(eq(10L), any(LocalDate.class), any(LocalDate.class), eq(2)))
                .thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.updateExisting(1L, request);

        assertTrue(result.isAdditionalPaymentRequired());
        assertNotNull(result.getUpdatePaymentDeadline());
        assertEquals(new BigDecimal("300.00"), result.getTotalPrice());
        verify(bookingRepository, times(2)).save(any(Booking.class));
    }

    @Test
    void updateExisting_shouldRefund_whenConfirmedPriceDecreases() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setTotalPrice(BigDecimal.valueOf(300));
        booking.setCheckInDate(LocalDate.now().plusDays(40));
        booking.setCheckOutDate(LocalDate.now().plusDays(42));
        booking.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));
        booking.setAppUser(user);

        Booking request = new Booking();
        request.setAppUser(user);
        request.setHotel(hotel);
        request.setCheckInDate(booking.getCheckInDate());
        request.setCheckOutDate(booking.getCheckOutDate());
        request.setNumberOfAdults(2);
        request.setNumberOfChildren(1);
        request.setNumberOfGuests(3);

        BookingRoom reqRoom = new BookingRoom();
        reqRoom.setRoomType(roomType);
        reqRoom.setNumberOfRooms(1);
        request.setBookingRooms(new ArrayList<>(List.of(reqRoom)));

        Payment payment = new Payment();
        payment.setId(55L);
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAmount(BigDecimal.valueOf(300));
        payment.setRefundAmount(BigDecimal.ZERO);

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(enhancedPricingService.calculateTotalStayPrice(any(Booking.class), any(Hotel.class), any(RoomType.class), eq(1)))
                .thenReturn(BigDecimal.valueOf(200));
        when(availabilityService.isNumberOfGuestsValid(any(Booking.class))).thenReturn(true);
        when(availabilityService.checkAvailability(eq(10L), any(LocalDate.class), any(LocalDate.class), eq(1)))
                .thenReturn(true);
        when(paymentRepository.existsByBookingId(1L)).thenReturn(true);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.updateExisting(1L, request);

        assertEquals(new BigDecimal("200.00"), result.getTotalPrice());
        assertFalse(result.isAdditionalPaymentRequired());

        ArgumentCaptor<RefundRequestDto> captor = ArgumentCaptor.forClass(RefundRequestDto.class);
        verify(paymentService).refundPayment(captor.capture());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(captor.getValue().getRefundAmount()));
    }

    @Test
    void checkInBooking_shouldSucceed_whenConfirmedAndEligible() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now());
        booking.setAdditionalPaymentRequired(false);

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.checkInBooking(1L);

        assertEquals(Booking.BookingStatus.CHECKED_IN, result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void checkInBooking_shouldThrow_whenAdditionalPaymentRequired() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now());
        booking.setAdditionalPaymentRequired(true);

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> bookingService.checkInBooking(1L)
        );

        assertTrue(ex.getMessage().contains("Additional payment is required"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void checkInBooking_shouldThrow_whenNotConfirmed() {
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setCheckInDate(LocalDate.now());
        booking.setAdditionalPaymentRequired(false);

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> bookingService.checkInBooking(1L)
        );

        assertTrue(ex.getMessage().contains("Only confirmed bookings"));
    }

    @Test
    void checkInBooking_shouldThrow_whenCheckInDateIsInFuture() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now().plusDays(3));
        booking.setAdditionalPaymentRequired(false);

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> bookingService.checkInBooking(1L)
        );

        assertTrue(ex.getMessage().contains("Cannot check in before check-in date"));
    }

    @Test
    void checkOutBooking_shouldSucceed_whenCheckedIn() {
        booking.setStatus(Booking.BookingStatus.CHECKED_IN);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.checkOutBooking(1L);

        assertEquals(Booking.BookingStatus.CHECKED_OUT, result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void checkOutBooking_shouldThrow_whenNotCheckedIn() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> bookingService.checkOutBooking(1L)
        );

        assertTrue(ex.getMessage().contains("Only checked-in bookings"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void getGuestHistory_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> expected = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);

        Page<Booking> result = bookingService.getGuestHistory(1L, pageable);

        assertEquals(1, result.getTotalElements());
        verify(bookingRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void filterBookings_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> expected = new PageImpl<>(List.of(booking));

        when(bookingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);

        Page<Booking> result = bookingService.filterBookings(
                1L,
                null,
                1L,
                null,
                Booking.BookingStatus.CONFIRMED,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(30),
                100.0,
                500.0,
                pageable
        );

        assertEquals(1, result.getContent().size());
        verify(bookingRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void cancelExpiredPendingBookings_shouldAutoCancelExpiredBookings() {
        Booking expired = new Booking();
        expired.setId(2L);
        expired.setBookingReference("EXPIRED-REF");
        expired.setStatus(Booking.BookingStatus.PENDING);
        expired.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        when(bookingRepository.findByStatusAndCreatedAtBefore(eq(Booking.BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(expired));

        bookingService.cancelExpiredPendingBookings();

        assertEquals(Booking.BookingStatus.CANCELLED, expired.getStatus());
        assertEquals("SYSTEM", expired.getCancelledBy());
        assertTrue(expired.getCancelReason().contains("Auto-cancelled"));
        verify(bookingRepository).save(expired);
    }

    @Test
    void cancelExpiredPendingBookings_shouldDoNothing_whenNoExpiredBookings() {
        when(bookingRepository.findByStatusAndCreatedAtBefore(eq(Booking.BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of());

        bookingService.cancelExpiredPendingBookings();

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void revertExpiredUpdatePayments_shouldRestoreSnapshotAndDeleteIt() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setAdditionalPaymentRequired(true);
        booking.setUpdatePaymentDeadline(LocalDateTime.now().minusMinutes(10));
        booking.setCheckInDate(LocalDate.now().plusDays(20));
        booking.setCheckOutDate(LocalDate.now().plusDays(25));
        booking.setNumberOfGuests(4);
        booking.setNumberOfAdults(2);
        booking.setNumberOfChildren(2);
        booking.setTotalPrice(BigDecimal.valueOf(500));
        booking.setBookingRooms(new ArrayList<>());

        Booking snapshot = new Booking();
        snapshot.setId(99L);
        snapshot.setActive(false);
        snapshot.setSnapshotOf(booking);
        snapshot.setBookingReference(booking.getBookingReference());
        snapshot.setCheckInDate(LocalDate.now().plusDays(15));
        snapshot.setCheckOutDate(LocalDate.now().plusDays(18));
        snapshot.setNumberOfGuests(3);
        snapshot.setNumberOfAdults(2);
        snapshot.setNumberOfChildren(1);
        snapshot.setTotalPrice(BigDecimal.valueOf(300));

        BookingRoom snapshotRoom = new BookingRoom();
        snapshotRoom.setRoomType(roomType);
        snapshotRoom.setNumberOfRooms(1);
        snapshotRoom.setBasePricePerNightPerRoom(BigDecimal.valueOf(120));
        snapshotRoom.setTotalPriceWithFees(BigDecimal.valueOf(300));
        snapshot.setBookingRooms(new ArrayList<>(List.of(snapshotRoom)));

        Payment payment = new Payment();
        payment.setId(77L);
        payment.setStatus(Payment.PaymentStatus.PARTIALLY_PAID);
        payment.setTotalAmount(BigDecimal.valueOf(500));

        when(bookingRepository.findConfirmedBookingsWithExpiredUpdateDeadline(any(LocalDateTime.class)))
                .thenReturn(List.of(booking));
        when(bookingRepository.findInactiveSnapshotByOriginalReference(booking.getBookingReference()))
                .thenReturn(Optional.of(snapshot));
        when(paymentRepository.existsByBookingId(booking.getId())).thenReturn(true);
        when(paymentRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(payment));

        bookingService.revertExpiredUpdatePayments();

        assertFalse(booking.isAdditionalPaymentRequired());
        assertNull(booking.getUpdatePaymentDeadline());
        assertEquals(snapshot.getCheckInDate(), booking.getCheckInDate());
        assertEquals(snapshot.getCheckOutDate(), booking.getCheckOutDate());
        assertEquals(snapshot.getTotalPrice(), booking.getTotalPrice());
        assertEquals(1, booking.getBookingRooms().size());

        assertEquals(Payment.PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals(snapshot.getTotalPrice(), payment.getTotalAmount());
        assertEquals(BigDecimal.ZERO, payment.getRequiredAdditionalPaymentAmount());

        verify(paymentRepository).save(payment);
        verify(bookingRepository).save(booking);
        verify(bookingRepository).delete(snapshot);
    }

    @Test
    void revertExpiredUpdatePayments_shouldSkip_whenNoExpiredUpdates() {
        when(bookingRepository.findConfirmedBookingsWithExpiredUpdateDeadline(any(LocalDateTime.class)))
                .thenReturn(List.of());

        bookingService.revertExpiredUpdatePayments();

        verify(bookingRepository, never()).save(any());
        verify(bookingRepository, never()).delete(any(Booking.class));
        verifyNoInteractions(paymentRepository);
    }
}