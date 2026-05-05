package com.worldcup.hotelbooking.reservation.booking;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    @Query("""
        SELECT b FROM Booking b
        WHERE b.id = :id
        AND b.active = true
    """)
    Optional<Booking> findActiveBookingById(@Param("id") Long id);

    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.id = :bookingId
        AND b.active = true
        AND b.hotel.owner.id = :ownerId
    """)
    boolean isActiveBookingOwnedByHotelOwner(@Param("bookingId") Long bookingId,
                                             @Param("ownerId") Long ownerId);

    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.bookingReference = :reference
        AND b.active = true
        AND b.hotel.owner.id = :ownerId
    """)
    boolean isActiveBookingByReferenceOwnedByHotelOwner(@Param("reference") String reference,
                                                        @Param("ownerId") Long ownerId);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.appUser u
        LEFT JOIN FETCH b.hotel h
        LEFT JOIN FETCH b.bookingRooms br
        LEFT JOIN FETCH br.roomType rt
        WHERE b.bookingReference = :bookingReference
        AND b.active = true
    """)
    Optional<Booking> findByBookingReferenceWithRooms(@Param("bookingReference") String bookingReference);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.appUser u
        LEFT JOIN FETCH b.hotel h
        LEFT JOIN FETCH b.bookingRooms br
        LEFT JOIN FETCH br.roomType rt
        WHERE b.bookingReference = :bookingReference
    """)
    Optional<Booking> findByBookingReference(@Param("bookingReference") String bookingReference);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.appUser u
        LEFT JOIN FETCH b.hotel h
        LEFT JOIN FETCH b.bookingRooms br
        LEFT JOIN FETCH br.roomType rt
        WHERE b.appUser.id = :userId
        AND b.status = :status
    """)
    List<Booking> findByAppUser_IdAndStatus(@Param("userId") Long userId,
                                            @Param("status") Booking.BookingStatus status);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.appUser u
        LEFT JOIN FETCH b.hotel h
        LEFT JOIN FETCH b.bookingRooms br
        LEFT JOIN FETCH br.roomType rt
        WHERE b.appUser.id = :userId
    """)
    List<Booking> findByAppUser_Id(@Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.appUser u
        LEFT JOIN FETCH b.hotel h
        LEFT JOIN FETCH b.bookingRooms br
        LEFT JOIN FETCH br.roomType rt
        WHERE b.hotel.id = :hotelId
        AND b.status = :status
    """)
    List<Booking> findByHotel_IdAndStatus(@Param("hotelId") Long hotelId,
                                          @Param("status") Booking.BookingStatus status);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.appUser u
        LEFT JOIN FETCH b.hotel h
        LEFT JOIN FETCH b.bookingRooms br
        LEFT JOIN FETCH br.roomType rt
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
    Optional<Booking> findInactiveSnapshotByOriginalReference(@Param("bookingReference") String bookingReference);

    @Query("""
        SELECT h.owner.id FROM Booking b
        JOIN b.hotel h
        WHERE b.id = :id
    """)
    Optional<Long> findHotelOwnerIdByBookingId(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.appUser u
        LEFT JOIN FETCH b.hotel h
        LEFT JOIN FETCH b.bookingRooms br
        LEFT JOIN FETCH br.roomType rt
        WHERE b.id = :id
        AND b.active = true
    """)
    Optional<Booking> findByIdWithRooms(@Param("id") Long id);


}