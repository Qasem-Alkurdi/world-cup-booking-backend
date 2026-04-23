package com.worldcup.hotelbooking.reservation.booking;

import java.math.BigDecimal;

public interface BookingService {
    // 1. Create booking (most important!)
    Booking createBooking(Booking booking);

    // 2. Get booking by ID
    Booking getBookingById(Long id);


    // 5. Cancel booking
    Booking cancelBooking(Long id, String reason);

    // 6. Confirm booking (after payment)
    Booking confirmBooking(Long id);


    // 8. Calculate total price (helper method)
    BigDecimal calculateTotalPrice(Booking booking);


}
