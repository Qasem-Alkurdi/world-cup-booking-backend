package com.worldcup.hotelbooking.booking.booking;

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

    //  Booking findByBookingReference(String bookingReference);

    List<Booking> findByStatusAndCreatedAtBefore(Booking.BookingStatus status, LocalDateTime createdAt);

}