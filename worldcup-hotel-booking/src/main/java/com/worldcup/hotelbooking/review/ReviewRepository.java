package com.worldcup.hotelbooking.review;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByHotelAndVisibleTrueOrderByCreatedAtDesc(Hotel hotel);

    Optional<Review> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);
}