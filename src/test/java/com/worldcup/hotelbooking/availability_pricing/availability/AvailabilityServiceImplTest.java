package com.worldcup.hotelbooking.availability_pricing.availability;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import com.worldcup.hotelbooking.catalog.roomtype.exception.RoomTypeNotFoundException;
import com.worldcup.hotelbooking.reservation.booking.Booking;
import com.worldcup.hotelbooking.reservation.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.reservation.bookingroom.BookingRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceImplTest {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private BookingRoomRepository bookingRoomRepository;

    private AvailabilityServiceImpl availabilityService;
    private LocalDate checkIn;
    private LocalDate checkOut;

    @BeforeEach
    void setUp() {
        availabilityService = new AvailabilityServiceImpl(roomTypeRepository, bookingRoomRepository);
        checkIn = LocalDate.now().plusDays(10);
        checkOut = LocalDate.now().plusDays(15);
    }

    @Test
    void checkRoomTypeAvailability_returnsTrue_whenRoomsAreAvailable() {
        // Arrange
        RoomType roomType = createRoomType(1L, 10, 2, 2);
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));
        when(bookingRoomRepository.countBookedRooms(1L, checkIn, checkOut)).thenReturn(4);

        // Act
        boolean result = availabilityService.checkRoomTypeAvailability(1L, checkIn, checkOut);

        // Assert
        assertTrue(result);
    }

    @Test
    void checkRoomTypeAvailability_returnsFalse_whenNoRoomsAreAvailable() {
        // Arrange
        RoomType roomType = createRoomType(1L, 5, 2, 2);
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));
        when(bookingRoomRepository.countBookedRooms(1L, checkIn, checkOut)).thenReturn(5);

        // Act
        boolean result = availabilityService.checkRoomTypeAvailability(1L, checkIn, checkOut);

        // Assert
        assertFalse(result);
    }

    @Test
    void checkRoomTypeAvailability_throwsRoomTypeNotFoundException_whenRoomTypeDoesNotExist() {
        // Arrange
        when(roomTypeRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(RoomTypeNotFoundException.class,
                () -> availabilityService.checkRoomTypeAvailability(99L, checkIn, checkOut));
    }

    @Test
    void getAvailableRooms_returnsCorrectAvailableRoomsCount() {
        // Arrange
        RoomType roomType = createRoomType(1L, 12, 2, 2);
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));
        when(bookingRoomRepository.countBookedRooms(1L, checkIn, checkOut)).thenReturn(7);

        // Act
        int result = availabilityService.getAvailableRooms(1L, checkIn, checkOut);

        // Assert
        assertEquals(5, result);
    }

    @Test
    void getAvailableRooms_throwsRoomTypeNotFoundException_whenRoomTypeDoesNotExist() {
        // Arrange
        when(roomTypeRepository.findById(77L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(RoomTypeNotFoundException.class,
                () -> availabilityService.getAvailableRooms(77L, checkIn, checkOut));
    }

    @Test
    void checkAvailability_returnsTrue_whenRequestedRoomsAreAvailable() {
        // Arrange
        RoomType roomType = createRoomType(1L, 10, 2, 2);
        when(bookingRoomRepository.countBookedRooms(1L, checkIn, checkOut)).thenReturn(6);
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));

        // Act
        boolean result = availabilityService.checkAvailability(1L, checkIn, checkOut, 4);

        // Assert
        assertTrue(result);
    }

    @Test
    void checkAvailability_returnsFalse_whenRequestedRoomsExceedAvailability() {
        // Arrange
        RoomType roomType = createRoomType(1L, 10, 2, 2);
        when(bookingRoomRepository.countBookedRooms(1L, checkIn, checkOut)).thenReturn(8);
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));

        // Act
        boolean result = availabilityService.checkAvailability(1L, checkIn, checkOut, 3);

        // Assert
        assertFalse(result);
    }

    @Test
    void isNumberOfGuestsValid_returnsTrue_whenGuestsFitInBookedRooms() {
        // Arrange
        Booking booking = new Booking();
        booking.setNumberOfAdults(3);
        booking.setNumberOfChildren(2);

        RoomType roomType = createRoomType(1L, 5, 2, 1);
        BookingRoom bookingRoom = createBookingRoom(roomType, 2);
        booking.setBookingRooms(List.of(bookingRoom));

        // Act
        boolean result = availabilityService.isNumberOfGuestsValid(booking);

        // Assert
        assertTrue(result);
    }

    @Test
    void isNumberOfGuestsValid_returnsFalse_whenGuestsExceedRoomCapacity() {
        // Arrange
        Booking booking = new Booking();
        booking.setNumberOfAdults(5);
        booking.setNumberOfChildren(3);

        RoomType roomType = createRoomType(1L, 5, 2, 1);
        BookingRoom bookingRoom = createBookingRoom(roomType, 2);
        booking.setBookingRooms(List.of(bookingRoom));

        // Act
        boolean result = availabilityService.isNumberOfGuestsValid(booking);

        // Assert
        assertFalse(result);
    }

    @Test
    void checkAvailabilityOfHotel_returnsTrue_whenAtLeastOneRoomTypeIsAvailable() {
        // Arrange
        Hotel hotel = new Hotel();
        RoomType unavailableRoomType = createRoomType(1L, 5, 2, 1);
        RoomType availableRoomType = createRoomType(2L, 3, 2, 1);

        unavailableRoomType.setHotel(hotel);
        availableRoomType.setHotel(hotel);
        hotel.setRoomTypes(new ArrayList<>(List.of(unavailableRoomType, availableRoomType)));

        when(bookingRoomRepository.countBookedRooms(1L, checkIn, checkOut)).thenReturn(5);
        when(bookingRoomRepository.countBookedRooms(2L, checkIn, checkOut)).thenReturn(1);
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(unavailableRoomType));
        when(roomTypeRepository.findById(2L)).thenReturn(Optional.of(availableRoomType));

        // Act
        boolean result = availabilityService.checkAvailabilityOfHotel(hotel, checkIn, checkOut);

        // Assert
        assertTrue(result);
    }

    @Test
    void checkAvailabilityOfHotel_returnsFalse_whenNoRoomsAreAvailableInHotel() {
        // Arrange
        Hotel hotel = new Hotel();
        RoomType firstRoomType = createRoomType(1L, 2, 2, 1);
        RoomType secondRoomType = createRoomType(2L, 4, 2, 1);

        firstRoomType.setHotel(hotel);
        secondRoomType.setHotel(hotel);
        hotel.setRoomTypes(new ArrayList<>(List.of(firstRoomType, secondRoomType)));

        when(bookingRoomRepository.countBookedRooms(1L, checkIn, checkOut)).thenReturn(2);
        when(bookingRoomRepository.countBookedRooms(2L, checkIn, checkOut)).thenReturn(4);
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(firstRoomType));
        when(roomTypeRepository.findById(2L)).thenReturn(Optional.of(secondRoomType));

        // Act
        boolean result = availabilityService.checkAvailabilityOfHotel(hotel, checkIn, checkOut);

        // Assert
        assertFalse(result);
    }

    private RoomType createRoomType(Long id, int totalRooms, int maxAdults, int maxChildren) {
        RoomType roomType = new RoomType();
        roomType.setId(id);
        roomType.setName("RoomType-" + id);
        roomType.setTotalRooms(totalRooms);
        roomType.setMaxAdults(maxAdults);
        roomType.setMaxChildren(maxChildren);
        roomType.setBasePrice(BigDecimal.valueOf(100));
        roomType.setCurrency("USD");
        return roomType;
    }

    private BookingRoom createBookingRoom(RoomType roomType, int numberOfRooms) {
        BookingRoom bookingRoom = new BookingRoom();
        bookingRoom.setRoomType(roomType);
        bookingRoom.setNumberOfRooms(numberOfRooms);
        bookingRoom.setBasePricePerNightPerRoom(BigDecimal.valueOf(100));
        bookingRoom.setTotalPriceWithFees(BigDecimal.valueOf(200));
        return bookingRoom;
    }
}

