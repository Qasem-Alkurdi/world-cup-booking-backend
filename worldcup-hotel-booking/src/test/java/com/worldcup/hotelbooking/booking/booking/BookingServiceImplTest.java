package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.availability_pricing.availability.AvailabilityServiceImpl;
import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingServiceImpl;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.cancellation.CancellationPolicyServiceImpl;
import com.worldcup.hotelbooking.booking.cancellation.CancellationResponseDto;
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
        booking.setBookingRooms(List.of(bookingRoom));
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setTotalPrice(BigDecimal.valueOf(240));
        booking.setCreatedAt(LocalDateTime.now().minusHours(1));
    }

    @Test
    void getBookingById_shouldReturnBooking_whenExists() {
        // Arrange
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        // Act
        Booking result = bookingService.getBookingById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(bookingRepository).findByIdWithRooms(1L);
    }

    @Test
    void getBookingById_shouldThrow_whenNotFound() {
        // Arrange
        when(bookingRepository.findByIdWithRooms(99L)).thenReturn(Optional.empty());

        // Act + Assert
        BookingNotFoundException ex = assertThrows(BookingNotFoundException.class,
                () -> bookingService.getBookingById(99L));
        assertTrue(ex.getMessage().contains("99"));
        verify(bookingRepository).findByIdWithRooms(99L);
    }

    @Test
    void createBooking_shouldSucceed_whenValidData() {
        // Arrange
        when(availabilityService.isNumberOfGuestsValid(booking)).thenReturn(true);
        when(availabilityService.checkAvailability(eq(10L), any(LocalDate.class), any(LocalDate.class), eq(1)))
                .thenReturn(true);
        when(enhancedPricingService.calculateTotalStayPrice(any(Booking.class), any(Hotel.class), any(RoomType.class), eq(1)))
                .thenReturn(BigDecimal.valueOf(240));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking result = bookingService.createBooking(booking);

        // Assert
        assertEquals(Booking.BookingStatus.PENDING, result.getStatus());
        assertEquals(BigDecimal.valueOf(240.00).setScale(2), result.getTotalPrice());
        assertNotNull(result.getConfirmationDeadline());
        verify(availabilityService).isNumberOfGuestsValid(booking);
        verify(availabilityService).checkAvailability(10L, booking.getCheckInDate(), booking.getCheckOutDate(), 1);
        verify(enhancedPricingService).calculateTotalStayPrice(any(Booking.class), eq(hotel), eq(roomType), eq(1));
        verify(bookingRepository).save(booking);
    }

    @Test
    void createBooking_shouldThrow_whenCheckoutBeforeCheckin() {
        // Arrange
        booking.setCheckInDate(LocalDate.now().plusDays(5));
        booking.setCheckOutDate(LocalDate.now().plusDays(4));

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(booking));
        assertTrue(ex.getMessage().contains("Check-out"));
        verifyNoInteractions(availabilityService, enhancedPricingService, bookingRepository);
    }

    @Test
    void createBooking_shouldThrow_whenNoRooms() {
        // Arrange
        booking.setBookingRooms(List.of());

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(booking));
        assertTrue(ex.getMessage().contains("At least one room"));
        verifyNoInteractions(availabilityService, enhancedPricingService, bookingRepository);
    }

    @Test
    void createBooking_shouldThrow_whenInvalidNumberOfGuests() {
        // Arrange
        when(availabilityService.isNumberOfGuestsValid(booking)).thenReturn(false);

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(booking));
        assertTrue(ex.getMessage().contains("exceeds room capacity"));
        verify(availabilityService).isNumberOfGuestsValid(booking);
        verifyNoMoreInteractions(availabilityService);
        verifyNoInteractions(enhancedPricingService, bookingRepository);
    }

    @Test
    void createBooking_shouldThrow_whenRoomNotAvailable() {
        // Arrange
        when(availabilityService.isNumberOfGuestsValid(booking)).thenReturn(true);
        when(availabilityService.checkAvailability(eq(10L), any(LocalDate.class), any(LocalDate.class), eq(1)))
                .thenReturn(false);

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(booking));
        assertTrue(ex.getMessage().contains("Not enough rooms available"));
        verify(availabilityService).isNumberOfGuestsValid(booking);
        verify(availabilityService).checkAvailability(10L, booking.getCheckInDate(), booking.getCheckOutDate(), 1);
        verifyNoInteractions(enhancedPricingService, bookingRepository);
    }

    @Test
    void cancelBooking_shouldSucceedAndRefund_whenPolicyAllowsAndPaymentExists() {
        // Arrange
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        CancellationResponseDto cancellation = CancellationResponseDto.builder()
                .canCancel(true)
                .refundPercentage(100)
                .refundAmount(BigDecimal.valueOf(240))
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
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking result = bookingService.cancelBooking(1L, "User request");

        // Assert
        assertEquals(Booking.BookingStatus.CANCELLED, result.getStatus());
        assertTrue(result.getCancelReason().contains("User request"));
        assertNotNull(result.getCancelledAt());
        assertEquals("test-user", result.getCancelledBy());

        ArgumentCaptor<RefundRequestDto> captor = ArgumentCaptor.forClass(RefundRequestDto.class);
        verify(paymentService).refundPayment(captor.capture());
        assertEquals(BigDecimal.valueOf(240), captor.getValue().getRefundAmount());
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBooking_shouldThrow_whenBookingNotFound() {
        // Arrange
        when(bookingRepository.findByIdWithRooms(100L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(BookingNotFoundException.class,
                () -> bookingService.cancelBooking(100L, "Any reason"));
        verify(bookingRepository).findByIdWithRooms(100L);
        verifyNoMoreInteractions(bookingRepository);
        verifyNoInteractions(cancellationPolicyService, paymentRepository, paymentService);
    }

    @Test
    void cancelBooking_shouldThrow_whenPolicyDoesNotAllowCancellation() {
        // Arrange
        CancellationResponseDto cancellation = CancellationResponseDto.builder()
                .canCancel(false)
                .refundPercentage(0)
                .refundAmount(BigDecimal.ZERO)
                .cancellationFee(BigDecimal.valueOf(100))
                .policyMessage("Cannot cancel within 24 hours")
                .build();

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(cancellationPolicyService.previewCancellation(booking)).thenReturn(cancellation);

        // Act + Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.cancelBooking(1L, "Late cancellation"));
        assertTrue(ex.getMessage().contains("Cannot cancel"));
        verify(bookingRepository).findByIdWithRooms(1L);
        verify(cancellationPolicyService).previewCancellation(booking);
        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(paymentRepository, paymentService);
    }

    @Test
    void confirmBooking_shouldSucceed_whenPending() {
        // Arrange
        booking.setStatus(Booking.BookingStatus.PENDING);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking result = bookingService.confirmBooking(1L);

        // Assert
        assertEquals(Booking.BookingStatus.CONFIRMED, result.getStatus());
        assertNotNull(result.getConfirmedAt());
        verify(bookingRepository).save(booking);
    }

    @Test
    void confirmBooking_shouldThrow_whenAlreadyConfirmed() {
        // Arrange
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        // Act + Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.confirmBooking(1L));
        assertTrue(ex.getMessage().contains("already confirmed"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void confirmBooking_shouldThrow_whenCancelled() {
        // Arrange
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        // Act + Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.confirmBooking(1L));
        assertTrue(ex.getMessage().contains("cannot be confirmed"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateExisting_shouldSucceed_whenValidRequestAndPriceChanges() {
        // Arrange
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setTotalPrice(BigDecimal.valueOf(200));
        booking.setBookingRooms(new java.util.ArrayList<>(List.of(bookingRoom)));
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
        request.setBookingRooms(new java.util.ArrayList<>(List.of(reqRoom)));

        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(enhancedPricingService.calculateTotalStayPrice(any(Booking.class), any(Hotel.class), any(RoomType.class), eq(1)))
                .thenReturn(BigDecimal.valueOf(250));
        when(availabilityService.isNumberOfGuestsValid(any(Booking.class))).thenReturn(true);
        when(availabilityService.checkAvailability(eq(10L), any(LocalDate.class), any(LocalDate.class), eq(1)))
                .thenReturn(true);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking result = bookingService.updateExisting(1L, request);

        // Assert
        assertEquals(BigDecimal.valueOf(250.00).setScale(2), result.getTotalPrice());
        assertEquals(request.getCheckInDate(), result.getCheckInDate());
        assertEquals(request.getCheckOutDate(), result.getCheckOutDate());
        verify(bookingRepository).findByIdWithRooms(1L);
        verify(bookingRepository).save(booking);
    }

    @Test
    void checkInBooking_shouldSucceed_whenConfirmedAndEligible() {
        // Arrange
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setCheckInDate(LocalDate.now());
        booking.setAdditionalPaymentRequired(false);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking result = bookingService.checkInBooking(1L);

        // Assert
        assertEquals(Booking.BookingStatus.CHECKED_IN, result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void checkInBooking_shouldThrow_whenNotConfirmedOrAdditionalPaymentRequired() {
        // Arrange
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setAdditionalPaymentRequired(true);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        // Act + Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.checkInBooking(1L));
        assertTrue(ex.getMessage().contains("Additional payment is required"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void checkOutBooking_shouldSucceed_whenCheckedIn() {
        // Arrange
        booking.setStatus(Booking.BookingStatus.CHECKED_IN);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking result = bookingService.checkOutBooking(1L);

        // Assert
        assertEquals(Booking.BookingStatus.CHECKED_OUT, result.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void checkOutBooking_shouldThrow_whenNotCheckedIn() {
        // Arrange
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        when(bookingRepository.findByIdWithRooms(1L)).thenReturn(Optional.of(booking));

        // Act + Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.checkOutBooking(1L));
        assertTrue(ex.getMessage().contains("Only checked-in bookings"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void getGuestHistory_shouldReturnBookingsPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> expected = new PageImpl<>(List.of(booking));
        when(bookingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);

        // Act
        Page<Booking> result = bookingService.getGuestHistory(1L, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        verify(bookingRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void filterBookings_shouldReturnFilteredPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> expected = new PageImpl<>(List.of(booking));
        when(bookingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expected);

        // Act
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

        // Assert
        assertEquals(1, result.getContent().size());
        verify(bookingRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void cancelExpiredPendingBookings_shouldAutoCancelExpiredPendingBookings() {
        // Arrange
        Booking expired = new Booking();
        expired.setId(2L);
        expired.setBookingReference("EXPIRED-REF");
        expired.setStatus(Booking.BookingStatus.PENDING);
        expired.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        when(bookingRepository.findByStatusAndCreatedAtBefore(eq(Booking.BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(expired));

        // Act
        bookingService.cancelExpiredPendingBookings();

        // Assert
        assertEquals(Booking.BookingStatus.CANCELLED, expired.getStatus());
        assertEquals("SYSTEM", expired.getCancelledBy());
        assertTrue(expired.getCancelReason().contains("Auto-cancelled"));
        verify(bookingRepository).save(expired);
    }

    @Test
    void previewCancellation_shouldReturnPolicyPreview() {
        // Arrange
        CancellationResponseDto cancellation = CancellationResponseDto.builder()
                .canCancel(true)
                .refundPercentage(75)
                .refundAmount(BigDecimal.valueOf(180))
                .cancellationFee(BigDecimal.valueOf(60))
                .policyMessage("75% refund - 14-29 days notice")
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(cancellationPolicyService.previewCancellation(booking)).thenReturn(cancellation);

        // Act
        CancellationResponseDto result = bookingService.previewCancellation(1L);

        // Assert
        assertTrue(result.isCanCancel());
        assertEquals(BigDecimal.valueOf(180), result.getRefundAmount());
        verify(bookingRepository).findById(1L);
        verify(cancellationPolicyService).previewCancellation(booking);
    }
}
