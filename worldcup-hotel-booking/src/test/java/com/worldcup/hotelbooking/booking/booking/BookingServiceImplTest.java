package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.availability_pricing.availability.AvailabilityServiceImpl;
import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingServiceImpl;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.cancellation.CancellationPolicyServiceImpl;
import com.worldcup.hotelbooking.booking.cancellation.CancellationResponse;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.payment.Payment;
import com.worldcup.hotelbooking.payment.PaymentRepository;
import com.worldcup.hotelbooking.payment.PaymentServiceImpl;
import com.worldcup.hotelbooking.payment.RefundRequestDto;
import com.worldcup.hotelbooking.user.user.AppUser;
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
import static org.mockito.ArgumentMatchers.*;
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

        BookingNotFoundException ex = assertThrows(BookingNotFoundException.class,
                () -> bookingService.getBookingById(99L));

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
    void createBooking_shouldThrow_whenGuestCountMismatch() {
        booking.setNumberOfGuests(4); // adults+children = 3
        when(availabilityService.isNumberOfGuestsValid(booking)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(booking));

        assertTrue(ex.getMessage().contains("sum of adults and children"));
        verifyNoInteractions(enhancedPricingService, bookingRepository);
    }

    @Test
    void createBooking_shouldThrow_whenCheckoutBeforeCheckin() {
        booking.setCheckInDate(LocalDate.now().plusDays(5));
        booking.setCheckOutDate(LocalDate.now().plusDays(4));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(booking));

        assertTrue(ex.getMessage().contains("Check-out"));
        verifyNoInteractions(availabilityService, enhancedPricingService, bookingRepository);
    }

    @Test
    void createBooking_shouldThrow_whenNoRooms() {
        booking.setBookingRooms(new ArrayList<>());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(booking));

        assertTrue(ex.getMessage().contains("At least one room"));
        verifyNoInteractions(availabilityService, enhancedPricingService, bookingRepository);
    }

    @Test
    void createBooking_shouldThrow_whenInvalidNumberOfGuests() {
        when(availabilityService.isNumberOfGuestsValid(booking)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(booking));

        assertTrue(ex.getMessage().contains("exceeds room capacity"));
        verifyNoInteractions(enhancedPricingService, bookingRepository);
    }

    @Test
    void createBooking_shouldThrow_whenRoomNotAvailable() {
        when(availabilityService.isNumberOfGuestsValid(booking)).thenReturn(true);
        when(availabilityService.checkAvailability(eq(10L), any(LocalDate.class), any(LocalDate.class), eq(1)))
                .thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(booking));

        assertTrue(ex.getMessage().contains("Not enough rooms available"));
        verifyNoInteractions(enhancedPricingService, bookingRepository);
    }

    @Test
    void cancelBooking_shouldSucceedAndRefund_whenPolicyAllowsAndPaymentCompleted() {
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
        assertTrue(result.getCancelReason().contains("User request"));
        assertEquals("test-user", result.getCancelledBy());

        ArgumentCaptor<RefundRequestDto> captor = ArgumentCaptor.forClass(RefundRequestDto.class);
        verify(paymentService).refundPayment(captor.capture());
        assertEquals(BigDecimal.valueOf(240), captor.getValue().getRefundAmount());
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBooking_shouldAlsoRefund_whenPaymentPartiallyRefunded() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        CancellationResponse cancellation = CancellationResponse.builder()
                .canCancel(true)
                .refundAmount(BigDecimal.valueOf(50))
                .refundPercentage(100)
                .cancellationFee(BigDecimal.ZERO)
                .policyMessage("Allowed")
                .build();

        Payment payment = new Payment();
        payment.setId(12L);
        payment.setStatus(Payment.PaymentStatus.PARTIALLY_REFUNDED);
        payment.setPaidAmount(BigDecimal.valueOf(200));
        payment.setRefundAmount(BigDecimal.valueOf(100));

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(cancellationPolicyService.previewCancellation(booking)).thenReturn(cancellation);
        when(paymentRepository.existsByBookingId(1L)).thenReturn(true);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancelBooking(1L, "reason");

        verify(paymentService).refundPayment(any(RefundRequestDto.class));
    }

    @Test
    void cancelBooking_shouldThrow_whenBookingNotFound() {
        when(bookingRepository.findByIdWithRooms(100L)).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class,
                () -> bookingService.cancelBooking(100L, "Any reason"));

        verifyNoInteractions(cancellationPolicyService, paymentRepository, paymentService);
    }

    @Test
    void cancelBooking_shouldThrow_whenPolicyDoesNotAllowCancellation() {
        CancellationResponse cancellation = CancellationResponse.builder()
                .canCancel(false)
                .refundAmount(BigDecimal.ZERO)
                .cancellationFee(BigDecimal.valueOf(100))
                .policyMessage("Cannot cancel within 24 hours")
                .build();

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(cancellationPolicyService.previewCancellation(booking)).thenReturn(cancellation);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.cancelBooking(1L, "Late cancellation"));

        assertTrue(ex.getMessage().contains("Cannot cancel"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void previewCancellation_shouldReturnPolicyPreview() {
        CancellationResponse cancellation = CancellationResponse.builder()
                .canCancel(true)
                .refundAmount(BigDecimal.valueOf(180))
                .refundPercentage(75)
                .cancellationFee(BigDecimal.valueOf(60))
                .policyMessage("75% refund - 14-29 days notice")
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(cancellationPolicyService.previewCancellation(booking)).thenReturn(cancellation);

        CancellationResponse result = bookingService.previewCancellation(1L);

        assertTrue(result.isCanCancel());
        assertEquals(BigDecimal.valueOf(180), result.getRefundAmount());
    }

    @Test
    void previewManagerCancellation_shouldReturnResponse() {
        CancellationResponse response = CancellationResponse.builder()
                .canCancel(true)
                .refundAmount(BigDecimal.valueOf(240))
                .policyMessage("Manager policy")
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(cancellationPolicyService.calculateManagerCancellation(booking)).thenReturn(response);

        CancellationResponse result = bookingService.previewManagerCancellation(1L);

        assertEquals("Manager policy", result.getPolicyMessage());
        verify(cancellationPolicyService).calculateManagerCancellation(booking);
    }

    @Test
    void cancelBookingByManager_shouldCancelAndSetCancelledByManager() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        CancellationResponse cancellation = CancellationResponse.builder()
                .canCancel(true)
                .refundAmount(BigDecimal.valueOf(200))
                .bonusAmount(BigDecimal.valueOf(20))
                .bonusTierDescription("Tier bonus")
                .build();

        Payment payment = new Payment();
        payment.setId(33L);
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAmount(BigDecimal.valueOf(200));
        payment.setRefundAmount(BigDecimal.ZERO);

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(cancellationPolicyService.calculateManagerCancellation(booking)).thenReturn(cancellation);
        when(paymentRepository.existsByBookingId(1L)).thenReturn(true);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.cancelBookingByManager(1L, "Ops issue", "manager1");

        assertEquals(Booking.BookingStatus.CANCELLED, result.getStatus());
        assertEquals("manager1", result.getCancelledBy());
        assertTrue(result.getCancelReason().contains("HOTEL_CANCELLED"));
        verify(paymentService).refundPayment(any(RefundRequestDto.class));
    }

    @Test
    void confirmBooking_shouldSucceed_whenPending() {
        booking.setStatus(Booking.BookingStatus.PENDING);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.confirmBooking(1L);

        assertEquals(Booking.BookingStatus.CONFIRMED, result.getStatus());
        assertNotNull(result.getConfirmedAt());
    }

    @Test
    void confirmBooking_shouldThrow_whenAlreadyConfirmed() {
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.confirmBooking(1L));

        assertTrue(ex.getMessage().contains("already confirmed"));
    }

    @Test
    void confirmBooking_shouldThrow_whenCancelled() {
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.confirmBooking(1L));

        assertTrue(ex.getMessage().contains("cannot be confirmed"));
    }

    @Test
    void updateExisting_shouldSucceed_whenPendingAndValidRequest() {
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setTotalPrice(BigDecimal.valueOf(200));
        booking.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));

        Booking request = new Booking();
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

        Booking request = new Booking();
        request.setHotel(hotel);
        request.setCheckInDate(booking.getCheckInDate());
        request.setCheckOutDate(booking.getCheckOutDate());
        request.setNumberOfAdults(booking.getNumberOfAdults());
        request.setNumberOfChildren(booking.getNumberOfChildren());
        request.setNumberOfGuests(booking.getNumberOfGuests());
        request.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        ModificationNotAllowedException ex = assertThrows(ModificationNotAllowedException.class,
                () -> bookingService.updateExisting(1L, request));

        assertTrue(ex.getMessage().contains("outstanding payment"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateExisting_shouldThrow_whenHotelChanges() {
        Hotel anotherHotel = new Hotel();
        anotherHotel.setId(999L);

        Booking request = new Booking();
        request.setHotel(anotherHotel);
        request.setCheckInDate(booking.getCheckInDate());
        request.setCheckOutDate(booking.getCheckOutDate());
        request.setNumberOfAdults(booking.getNumberOfAdults());
        request.setNumberOfChildren(booking.getNumberOfChildren());
        request.setNumberOfGuests(booking.getNumberOfGuests());
        request.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));

        booking.setBookingRooms(new ArrayList<>(List.of(bookingRoom)));

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        ModificationNotAllowedException ex = assertThrows(ModificationNotAllowedException.class,
                () -> bookingService.updateExisting(1L, request));

        assertTrue(ex.getMessage().contains("Cannot modify the hotel"));
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

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.checkInBooking(1L));

        assertTrue(ex.getMessage().contains("Additional payment is required"));
        verify(bookingRepository, never()).save(any());
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

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.checkOutBooking(1L));

        assertTrue(ex.getMessage().contains("Only checked-in bookings"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void getGuestHistory_shouldReturnBookingsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> expected = new PageImpl<>(List.of(booking));
        when(bookingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);

        Page<Booking> result = bookingService.getGuestHistory(1L, pageable);

        assertEquals(1, result.getTotalElements());
        verify(bookingRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void filterBookings_shouldReturnFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> expected = new PageImpl<>(List.of(booking));
        when(bookingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);

        Page<Booking> result = bookingService.filterBookings(
                1L,
                1L,
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
    void cancelExpiredPendingBookings_shouldAutoCancelExpiredPendingBookings() {
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
    void revertExpiredUpdatePayments_shouldSkipWhenNoExpiredUpdates() {
        when(bookingRepository.findConfirmedBookingsWithExpiredUpdateDeadline(any(LocalDateTime.class)))
                .thenReturn(List.of());

        bookingService.revertExpiredUpdatePayments();

        verify(bookingRepository, never()).save(any());
        verify(bookingRepository, never()).delete(any(Booking.class));
        verifyNoInteractions(paymentRepository);
    }
}
