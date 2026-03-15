package com.worldcup.hotelbooking.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Boolean existsByBookingId(Long bookingId);

    Optional<Payment> findByPaymentIntentId(String paymentIntentId);

    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.booking b
        WHERE b.id = :bookingId
    """)
    Optional<Payment> findByBookingId(@Param("bookingId") Long bookingId);

    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.booking b
        JOIN FETCH b.appUser
        WHERE b.appUser.id = :userId
    """)
    List<Payment> findByBooking_AppUser_Id(@Param("userId") Long userId);

    @Query("""
        SELECT p FROM Payment p
        JOIN FETCH p.booking b
        JOIN FETCH b.hotel
        WHERE b.hotel.id = :hotelId
    """)
    List<Payment> findByBooking_Hotel_Id(@Param("hotelId") Long hotelId);
}