package com.worldcup.hotelbooking.reservation.bookingroom;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT  COALESCE(SUM(br.numberOfRooms), 0)
            FROM BookingRoom br
            WHERE br.roomType.id = :roomTypeId
            AND br.booking.status <> 'CANCELLED'
            AND br.booking.checkInDate < :checkOut
            AND br.booking.checkOutDate > :checkIn
            """)
    int countBookedRooms(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT  COALESCE(SUM(br.numberOfRooms), 0)
            FROM BookingRoom br
            WHERE br.roomType.id = :roomTypeId
            AND br.booking.id <> :excludeBookingId
            AND br.booking.status <> 'CANCELLED'
            AND br.booking.checkInDate < :checkOut
            AND br.booking.checkOutDate > :checkIn
            """)
    int countBookedRoomsExcluding(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludeBookingId") Long excludeBookingId
    );


    @Query("""
            SELECT br
            FROM BookingRoom br
            JOIN FETCH br.roomType
            """)
    List<BookingRoom> findAllWithRoomType();
}
