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

    // ─────────────────────────────────────────────────────────────────────────
    // WHY findActiveBookingById EXISTS
    // ─────────────────────────────────────────────────────────────────────────
    // The Booking entity has a self-referencing @OneToOne field `snapshotOf`.
    // When Hibernate resolves plain findById(), it generates a JOIN between the
    // booking table and itself via snapshot_of_booking_reference.  That JOIN
    // returns NO row for bookings that have no snapshot — so findById() returns
    // an empty Optional even though the booking exists in the database.
    //
    // This query bypasses the snapshot join entirely by selecting only on id
    // and active = true, which always works correctly regardless of whether a
    // snapshot row exists.  Use this query everywhere you only need to check
    // ownership or existence — not when you need the full room list.
    // ─────────────────────────────────────────────────────────────────────────
    @Query("""
        SELECT b FROM Booking b
        WHERE b.id = :id
        AND b.active = true
        """)
    Optional<Booking> findActiveBookingById(@Param("id") Long id);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.bookingRooms br
        LEFT JOIN FETCH br.roomType
        WHERE b.id = :id
        """)
    Optional<Booking> findByIdWithRooms(@Param("id") Long id);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.bookingRooms br
        LEFT JOIN FETCH br.roomType
        WHERE b.bookingReference = :bookingReference AND b.active = true
        """)
    Optional<Booking> findByBookingReferenceWithRooms(@Param("bookingReference") String bookingReference);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.bookingRooms br
        LEFT JOIN FETCH br.roomType
        WHERE b.bookingReference = :bookingReference
        """)
    Optional<Booking> findByBookingReference(@Param("bookingReference") String bookingReference);

    List<Booking> findByAppUser_IdAndStatus(Long userId, Booking.BookingStatus status);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.bookingRooms br
        LEFT JOIN FETCH br.roomType
        WHERE b.appUser.id = :userId
        """)
    List<Booking> findByAppUser_Id(@Param("userId") Long userId);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.bookingRooms br
        LEFT JOIN FETCH br.roomType
        WHERE b.hotel.id = :hotelId AND b.status = :status
        """)
    List<Booking> findByHotel_IdAndStatus(@Param("hotelId") Long hotelId,
                                          @Param("status") Booking.BookingStatus status);

    @Query("""
        SELECT b FROM Booking b
        LEFT JOIN FETCH b.bookingRooms br
        LEFT JOIN FETCH br.roomType
        WHERE b.hotel.id = :hotelId
        """)
    List<Booking> findByHotel_Id(@Param("hotelId") long hotelId);

    boolean existsByHotel_IdAndStatusIn(Long hotelId, List<Booking.BookingStatus> statuses);

    List<Booking> findByStatusAndCreatedAtBefore(Booking.BookingStatus status, LocalDateTime createdAt);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.active = true
        AND b.status = 'CONFIRMED'
        AND b.additionalPaymentRequired = true
        AND b.updatePaymentDeadline IS NOT NULL
        AND b.updatePaymentDeadline < :now
        """)
    List<Booking> findConfirmedBookingsWithExpiredUpdateDeadline(@Param("now") LocalDateTime now);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.active = false
        AND b.snapshotOf.bookingReference = :bookingReference
        """)
    Optional<Booking> findInactiveSnapshotByOriginalReference(
            @Param("bookingReference") String bookingReference);
}