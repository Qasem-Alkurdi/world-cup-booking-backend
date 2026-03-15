
package com.worldcup.hotelbooking.booking.booking;
import com.worldcup.hotelbooking.availability_pricing.availability.AvailabilityServiceImpl;
import com.worldcup.hotelbooking.availability_pricing.match.MatchRepository;
import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingServiceImpl;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRepository;
import com.worldcup.hotelbooking.booking.cancellation.CancellationPolicyServiceImpl;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import com.worldcup.hotelbooking.user.user.AppUser;
import com.worldcup.hotelbooking.user.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingServiceImp
 *
 * Tests all business logic without touching the database
 * Uses Mockito to mock dependencies
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Booking Service Tests")
class BookingServiceImpTest {

    // Mocks (fake objects for dependencies)
    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private BookingRoomRepository bookingRoomRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private EnhancedPricingServiceImpl enhancedPricingService;

    @Mock
    private CancellationPolicyServiceImpl cancellationPolicyService;

    @Mock
    private AvailabilityServiceImpl availabilityService;

    // Service under test (with mocks injected)
    @InjectMocks
    private BookingServiceImpl bookingService;

    // Test data
    private Booking testBooking;
    private AppUser testUser;
    private Hotel testHotel;
    private RoomType testRoomType;
    private BookingRoom testBookingRoom;

    /**
     * Set up test data before each test
     * Runs before EVERY @Test method
     */
    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setUsername("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setBookings(new ArrayList<>());


        // Create test hotel
        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setName("Test Hotel");
        testHotel.setLatitude(40.7128);
        testHotel.setLongitude(-74.0060);
        testHotel.setBookings(new ArrayList<>());

        // Create test room type
        testRoomType = new RoomType();
        testRoomType.setId(1L);
        testRoomType.setName("Deluxe Room");
        testRoomType.setBasePrice(BigDecimal.valueOf(200));
        testRoomType.setMaxAdults(2);
        testRoomType.setMaxChildren(1);
        testRoomType.setTotalRooms(10);

        // Create test booking room
        testBookingRoom = new BookingRoom();
        testBookingRoom.setRoomType(testRoomType);
        testBookingRoom.setNumberOfRooms(1);
        testBookingRoom.setBasePricePerNightPerRoom(BigDecimal.valueOf(200));

        // Create test booking
        testBooking = new Booking();
        testBooking.setId(1L);
        testBooking.setBookingReference("WC2026-TEST123");
        testBooking.setAppUser(testUser);
        testBooking.setHotel(testHotel);
        testBooking.setCheckInDate(LocalDate.now().plusDays(30));
        testBooking.setCheckOutDate(LocalDate.now().plusDays(33));
        testBooking.setNumberOfGuests(2);
        testBooking.setNumberOfAdults(2);
        testBooking.setNumberOfChildren(0);
        testBooking.setStatus(Booking.BookingStatus.PENDING);
        testBooking.setTotalPrice(BigDecimal.valueOf(600));

        List<BookingRoom> rooms = new ArrayList<>();
        testBookingRoom.setBooking(testBooking);
        rooms.add(testBookingRoom);
        testBooking.setBookingRooms(rooms);
    }

    // ============ TEST: Get Booking By ID ============

    @Test
    @DisplayName("Should return booking when valid ID is provided")
    void getBookingById_ValidId_ReturnsBooking() {
        // Arrange (Given)
        Long bookingId = 1L;
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(testBooking));

        // Act (When)
        Booking result = bookingService.getBookingById(bookingId);

        // Assert (Then)
        assertNotNull(result);
        assertEquals(bookingId, result.getId());
        assertEquals("WC2026-TEST123", result.getBookingReference());
        verify(bookingRepository, times(1)).findById(bookingId);
    }

    @Test
    @DisplayName("Should throw exception when booking not found")
    void getBookingById_InvalidId_ThrowsException() {
        // Arrange
        Long bookingId = 999L;
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BookingNotFoundException.class, () -> {
            bookingService.getBookingById(bookingId);
        });

        verify(bookingRepository, times(1)).findById(bookingId);
    }

    // ============ TEST: Create Booking ============

    @Test
    @DisplayName("Should create booking successfully with valid data")
    void createBooking_ValidData_CreatesBooking() {
        // Arrange
        when(bookingRoomRepository.countBookedRooms(anyLong(), any(), any()))
                .thenReturn(5); // 5 rooms already booked
        when(roomTypeRepository.findById(anyLong()))
                .thenReturn(Optional.of(testRoomType)); // 10 total rooms
        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(testBooking);
        when(enhancedPricingService.calculateTotalStayPrice(any(), any(), any(), anyInt()))
                .thenReturn(BigDecimal.valueOf(600));
        lenient().when(availabilityService.isNumberOfGuestsValid(any(Booking.class)))
                .thenReturn(true);
        lenient().when(availabilityService.checkAvailability(anyLong(),any(),any(),anyInt()))
                .thenReturn(true);

        // Act
        Booking result = bookingService.createBooking(testBooking);

        // Assert
        assertNotNull(result);
        assertEquals(Booking.BookingStatus.PENDING, result.getStatus());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw exception when check-out before check-in")
    void createBooking_InvalidDates_ThrowsException() {
        // Arrange
        testBooking.setCheckInDate(LocalDate.now().plusDays(5));
        testBooking.setCheckOutDate(LocalDate.now().plusDays(3)); // Before check-in!

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(testBooking);
        });

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when no rooms provided")
    void createBooking_NoRooms_ThrowsException() {
        // Arrange
        testBooking.setBookingRooms(new ArrayList<>()); // Empty list

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(testBooking);
        });

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when not enough rooms available")
    void createBooking_NotEnoughRooms_ThrowsException() {
        // Arrange
        when(bookingRoomRepository.countBookedRooms(anyLong(), any(), any()))
                .thenReturn(9); // 9 already booked
        when(roomTypeRepository.findById(anyLong()))
                .thenReturn(Optional.of(testRoomType)); // 10 total, only 1 available!

        testBookingRoom.setNumberOfRooms(2); // Trying to book 2 rooms

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(testBooking);
        });
    }

    @Test
    @DisplayName("Should throw exception when guests exceed capacity")
    void createBooking_TooManyGuests_ThrowsException() {
        // Arrange
        testBooking.setNumberOfAdults(5); // Room only fits 2 adults!

        when(bookingRoomRepository.countBookedRooms(anyLong(), any(), any()))
                .thenReturn(5);
        when(roomTypeRepository.findById(anyLong()))
                .thenReturn(Optional.of(testRoomType));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(testBooking);
        });
    }

    // ============ TEST: Confirm Booking ============

    @Test
    @DisplayName("Should confirm booking successfully")
    void confirmBooking_PendingBooking_ConfirmsSuccessfully() {
        // Arrange
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(testBooking);

        // Act
        Booking result = bookingService.confirmBooking(1L);

        // Assert
        assertEquals(Booking.BookingStatus.CONFIRMED, result.getStatus());
        assertNotNull(result.getConfirmedAt());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw exception when confirming already confirmed booking")
    void confirmBooking_AlreadyConfirmed_ThrowsException() {
        // Arrange
        testBooking.setStatus(Booking.BookingStatus.CONFIRMED);
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            bookingService.confirmBooking(1L);
        });

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when confirming cancelled booking")
    void confirmBooking_CancelledBooking_ThrowsException() {
        // Arrange
        testBooking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            bookingService.confirmBooking(1L);
        });
    }

    // ============ TEST: Cancel Booking ============

    @Test
    @DisplayName("Should cancel booking successfully")
    void cancelBooking_ValidBooking_CancelsSuccessfully() {
        // Arrange
        String reason = "Plans changed";
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(testBooking);

        // Act
        Booking result = bookingService.cancelBooking(1L, reason);

        // Assert
        assertEquals(Booking.BookingStatus.CANCELLED, result.getStatus());
        assertEquals(reason, result.getCancelReason());
        assertNotNull(result.getCancelledAt());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw exception when cancelling already cancelled booking")
    void cancelBooking_AlreadyCancelled_ThrowsException() {
        // Arrange
        testBooking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(testBooking));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            bookingService.cancelBooking(1L, "Reason");
        });
    }

    // ============ TEST: Check Availability ============

    @Test
    @DisplayName("Should return true when rooms are available")
    void checkAvailability_RoomsAvailable_ReturnsTrue() {
        // Arrange
        Long roomTypeId = 1L;
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = LocalDate.now().plusDays(33);
        int requestedRooms = 2;

        when(bookingRoomRepository.countBookedRooms(roomTypeId, checkIn, checkOut))
                .thenReturn(5); // 5 rooms booked
        when(roomTypeRepository.findById(roomTypeId))
                .thenReturn(Optional.of(testRoomType)); // 10 total rooms

        // Act
        boolean result = availabilityService.checkAvailability(roomTypeId, checkIn, checkOut, requestedRooms);

        // Assert
        assertTrue(result); // 10 - 5 = 5 available, requesting 2, should be true
    }

    @Test
    @DisplayName("Should return false when not enough rooms available")
    void checkAvailability_NotEnoughRooms_ReturnsFalse() {
        // Arrange
        Long roomTypeId = 1L;
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = LocalDate.now().plusDays(33);
        int requestedRooms = 8;

        when(bookingRoomRepository.countBookedRooms(roomTypeId, checkIn, checkOut))
                .thenReturn(7); // 7 rooms booked
        when(roomTypeRepository.findById(roomTypeId))
                .thenReturn(Optional.of(testRoomType)); // 10 total, only 3 available

        // Act
        boolean result = availabilityService.checkAvailability(roomTypeId, checkIn, checkOut, requestedRooms);

        // Assert
        assertFalse(result); // Requesting 8 but only 3 available
    }

    // ============ TEST: Calculate Total Price ============

    @Test
    @DisplayName("Should calculate total price correctly")
    void calculateTotalPrice_ValidBooking_ReturnsCorrectPrice() {
        // Arrange
        LocalDate checkIn = LocalDate.of(2026, 6, 10);
        LocalDate checkOut = LocalDate.of(2026, 6, 13); // 3 nights

        // 1 room × $200/night × 3 nights = $600

        // Act
        BigDecimal result = bookingService.calculateTotalPrice(testBooking);

        // Assert
        assertEquals(BigDecimal.valueOf(600), result);
    }

    @Test
    @DisplayName("Should throw exception when nights is zero or negative")
    void calculateTotalPrice_InvalidDates_ThrowsException() {
        // Arrange
        LocalDate checkIn = LocalDate.of(2026, 6, 10);
        LocalDate checkOut = LocalDate.of(2026, 6, 10); // Same day!

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.calculateTotalPrice(testBooking);
        });
    }

    // ============ TEST: Find Booking By Reference ============

    @Test
    @DisplayName("Should find booking by reference")
    void findBookingByReference_ValidReference_ReturnsBooking() {
        // Arrange
        String reference = "WC2026-TEST123";
        when(bookingRepository.findByBookingReference(reference))
                .thenReturn(Optional.of(testBooking));

        // Act
        Booking result = bookingService.findBookingByReference(reference);

        // Assert
        assertNotNull(result);
        assertEquals(reference, result.getBookingReference());
    }

    @Test
    @DisplayName("Should throw exception when reference not found")
    void findBookingByReference_InvalidReference_ThrowsException() {
        // Arrange
        String reference = "INVALID-REF";
        when(bookingRepository.findByBookingReference(reference))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BookingNotFoundException.class, () -> {
            bookingService.findBookingByReference(reference);
        });
    }
}
