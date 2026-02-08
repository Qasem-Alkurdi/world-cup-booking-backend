package com.worldcup.hotelbooking.booking.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String name);

    java.util.List<Booking> findByUserIdAndStatus(Long userId, String status);

    java.util.List<Booking> findByUserId(Long userId);

    java.util.List<Booking> findByHotelIdAndStatus(Long hotelId, String status);

    java.util.List<Booking> findByHotelId(Long hotelId);

    boolean existsByHotelIdAndStatusIn(
            Long hotelId,
            Collection<String> statuses
    );
}
