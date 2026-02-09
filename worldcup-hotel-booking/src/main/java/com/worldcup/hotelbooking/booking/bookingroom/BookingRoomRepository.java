package com.worldcup.hotelbooking.booking.bookingroom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {
    @Query("""
SELECT  COALESCE(SUM(br.numberOfRooms), 0)
FROM BookingRoom br
WHERE br.roomType.id = :roomTypeId
AND br.booking.status <> 'CANCELLED'
AND br.booking.checkInDate < :checkOut
AND br.booking.checkOutDate > :checkIn
""")
    int countBookedRooms(
            Long roomTypeId,
            LocalDate checkIn,
            LocalDate checkOut
    );

}
