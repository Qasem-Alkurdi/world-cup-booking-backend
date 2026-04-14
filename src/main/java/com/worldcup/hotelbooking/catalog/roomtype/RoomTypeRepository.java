package com.worldcup.hotelbooking.catalog.roomtype;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    @Query("""
            select rt
            from RoomType rt
            where rt.hotel.id = :hotelId
              and rt.hotel.isDeleted = false
            """)
    List<RoomType> findByHotelIdAndHotelNotDeleted(@Param("hotelId") Long hotelId);

    @Query("""
            select rt
            from RoomType rt
            where rt.id = :id
              and rt.hotel.id = :hotelId
              and rt.hotel.isDeleted = false
            """)
    Optional<RoomType> findByIdAndHotelIdAndHotelNotDeleted(
            @Param("id") Long id,
            @Param("hotelId") Long hotelId
    );

    @Query("""
            select (count(rt) > 0)
            from RoomType rt
            where rt.hotel.id = :hotelId
              and lower(rt.name) = lower(:name)
              and rt.hotel.isDeleted = false
            """)
    boolean existsByHotelIdAndNameIgnoreCaseAndHotelNotDeleted(
            @Param("hotelId") Long hotelId,
            @Param("name") String name
    );

    @Query("""
            select  COALESCE(sum(rt.totalRooms) , 0)
            from RoomType rt
            where rt.hotel.id = :hotelId
              and rt.id <> :id
              and rt.hotel.isDeleted = false
            """)
    int getTotalRoomsByHotelId(
            @Param("hotelId") Long hotelId
    );

    List<RoomType> findByHotelId(Long id);
}
