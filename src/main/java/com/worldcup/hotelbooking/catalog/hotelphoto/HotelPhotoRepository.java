package com.worldcup.hotelbooking.catalog.hotelphoto;

import com.worldcup.hotelbooking.catalog.hotelphoto.dto.HotelPrimaryPhotoProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HotelPhotoRepository extends JpaRepository<HotelPhoto, Long> {

    List<HotelPhoto> findByHotelIdOrderBySortOrderAscCreatedAtAsc(Long hotelId);

    Optional<HotelPhoto> findByIdAndHotelId(Long photoId, Long hotelId);

    Optional<HotelPhoto> findByHotelIdAndPrimaryTrue(Long hotelId);

    @Query("""
            select coalesce(max(hp.sortOrder), 0) + 1
            from HotelPhoto hp
            where hp.hotel.id = :hotelId
            """)
    Integer findNextSortOrderByHotelId(@Param("hotelId") Long hotelId);

    @Query("""
            select new com.worldcup.hotelbooking.catalog.hotelphoto.dto.HotelPrimaryPhotoProjection(
                hp.hotel.id,
                hp.storageKey
            )
            from HotelPhoto hp
            where hp.hotel.id in :hotelIds
              and hp.primary = true
            """)
    List<HotelPrimaryPhotoProjection> findPrimaryPhotosByHotelIds(@Param("hotelIds") List<Long> hotelIds);
}