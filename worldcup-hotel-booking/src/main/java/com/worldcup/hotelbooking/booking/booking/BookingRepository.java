package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.payment.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    @Query("""
select b from Booking b
left join fetch b.bookingRooms br
left join fetch br.roomType
where b.id = :id
""")
     Optional<Booking> findByIdWithRooms(@Param("id") Long id);

    @Query("""
select b from Booking b
left join fetch b.bookingRooms br
left join fetch br.roomType
where b.bookingReference = :bookingReference AND b.active = true
""")
    Optional<Booking> findByBookingReferenceWithRooms(@Param("bookingReference") String bookingReference);

    @Query("""
select b from Booking b
left join fetch b.bookingRooms br
left join fetch br.roomType
where b.bookingReference = :bookingReference
""")
    Optional<Booking> findByBookingReference(@Param("bookingReference") String bookingReference);

    List<Booking> findByAppUser_IdAndStatus(Long userId, Booking.BookingStatus status);

    @Query("""
select b from Booking b
left join fetch b.bookingRooms br
left join fetch br.roomType
where b.appUser.id = :userId
""")
    List<Booking> findByAppUser_Id(@Param("userId") Long userId);

@Query("""
select b from Booking b
left join fetch b.bookingRooms br
left join fetch br.roomType
where b.hotel.id = :hotelId and b.status = :status
""")
    List<Booking> findByHotel_IdAndStatus(@Param("hotelId") Long hotelId,@Param("status") Booking.BookingStatus status);

    @Query("""
select b from Booking b
left join fetch b.bookingRooms br
left join fetch br.roomType
where b.hotel.id = :hotelId
""")
    java.util.List<Booking> findByHotel_Id(@Param("hotelId") long hotelId);

    boolean existsByHotel_IdAndStatusIn(
            Long hotelId,
            List<Booking.BookingStatus> statuses
    );



    List<Booking> findByStatusAndCreatedAtBefore(Booking.BookingStatus status, LocalDateTime createdAt);

    // Used by revertExpiredUpdatePayments() scheduler.
    // Finds active CONFIRMED bookings where the 24h additional-payment window has expired.
    @Query("SELECT b FROM Booking b " +
            "WHERE b.active = true " +
            "AND b.status = 'CONFIRMED' " +
            "AND b.additionalPaymentRequired = true " +
            "AND b.updatePaymentDeadline IS NOT NULL " +
            "AND b.updatePaymentDeadline < :now")
    List<Booking> findConfirmedBookingsWithExpiredUpdateDeadline(
            @Param("now") LocalDateTime now);

    // ── Query 2 ──────────────────────────────────────────────────────────────
    // Used by revertExpiredUpdatePayments() and PaymentServiceImpl to load or
    // delete the inactive snapshot copy for a given booking.
    // We use bookingReference (the business key) — not the surrogate id — because
    // bookingReference is the stable, human-readable identifier shared by both
    // the original booking and its snapshot copy.
    @Query("SELECT b FROM Booking b " +
            "WHERE b.active = false " +
            "AND b.snapshotOf.bookingReference = :bookingReference")
    Optional<Booking> findInactiveSnapshotByOriginalReference(
            @Param("bookingReference") String bookingReference);


}