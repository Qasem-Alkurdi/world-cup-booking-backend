package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRequestDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BookingService {
    // 1. Create booking (most important!)
    Booking createBooking(Booking booking);

    // 2. Get booking by ID
    Booking getBookingById(Long id);

    // 3. Get booking by reference
   // Booking getBookingByReference(String reference);

    // 4. Get user's bookings
    List<Booking> getUserBookings(Long userId, Booking.BookingStatus status);
    List<Booking> getUserBookings(Long userId);// Overloaded method to get all bookings regardless of status

    // 5. Cancel booking
    Booking cancelBooking(Long id, String reason);

    // 6. Confirm booking (after payment)
    Booking confirmBooking(Long id);

    // 7. Check availability (helper method)
    boolean checkAvailability(Long roomTypeId, LocalDate checkIn, LocalDate checkOut, int rooms);

    // 8. Calculate total price (helper method)
    BigDecimal calculateTotalPrice(Booking booking, LocalDate checkIn, LocalDate checkOut);

    // 9. Get all bookings for a hotel (for hotel staff)
    List<Booking> getHotelBookings(Long hotelId, Booking.BookingStatus status);
    List<Booking> getHotelBookings(Long hotelId);// Overloaded method to get all bookings regardless of status

}
