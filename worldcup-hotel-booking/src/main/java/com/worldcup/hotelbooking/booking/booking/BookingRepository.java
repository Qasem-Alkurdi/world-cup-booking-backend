package com.worldcup.hotelbooking.booking.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String name);

    List<Booking> findByAppUser_IdAndStatus(Long userId, Booking.BookingStatus status);

    List<Booking> findByAppUser_Id(Long userId);

    List<Booking> findByHotel_IdAndStatus(Long hotelId, Booking.BookingStatus status);

    java.util.List<Booking> findByHotel_Id(Long hotelId);

    boolean existsByHotel_IdAndStatusIn(
            Long hotelId,
            List<Booking.BookingStatus> statuses
    );

    //  Booking findByBookingReference(String bookingReference);
}