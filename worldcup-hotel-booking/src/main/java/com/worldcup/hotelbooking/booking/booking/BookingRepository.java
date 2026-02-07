package com.worldcup.hotelbooking.booking.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    public Optional<Booking> findByBookingReference(String name);
    public java.util.List<Booking> findByUserIdAndStatus(Long userId, String status);
    public java.util.List<Booking> findByUserId(Long userId);
    public java.util.List<Booking> findByHotelIdAndStatus(Long hotelId, String status);
    public java.util.List<Booking> findByHotelId(Long hotelId);
}
