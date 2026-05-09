package com.worldcup.hotelbooking.catalog.hotel;

import com.worldcup.hotelbooking.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HotelRepository extends JpaRepository<Hotel, Long>, JpaSpecificationExecutor<Hotel> {
    boolean existsByIdAndOwnerIdAndStatusAndIsDeletedFalse(Long id, Long ownerId, HotelStatus status);

    List<Hotel> findByStatusAndIsDeletedFalse(HotelStatus status);

    Optional<Hotel> findByIdAndStatusAndIsDeletedFalse(Long id, HotelStatus status);

    List<Hotel> findByOwnerAndStatusAndIsDeletedFalse(AppUser owner, HotelStatus status);

    @Query(value = """
              /* language=SQL */
            SELECT ST_DistanceSphere(
                ST_SetSRID(ST_MakePoint(h.longitude, h.latitude), 4326),
                ST_SetSRID(ST_MakePoint(s.longitude, s.latitude), 4326)
            )
            FROM hotel h, stadiums s
            WHERE h.id = :hotelId
              AND s.id = :stadiumId
            """, nativeQuery = true)
    Double calculateDistanceInMeters(@Param("hotelId") Long hotelId,
                                     @Param("stadiumId") Long stadiumId);

    @Query("""
                select h
                from Hotel h
                left join fetch h.roomTypes
                where h.id = :id
                  and h.status = :status
                  and h.isDeleted = false
            """)
    Optional<Hotel> findByIdWithRoomTypes(
            @Param("id") Long id,
            @Param("status") HotelStatus status
    );
}
