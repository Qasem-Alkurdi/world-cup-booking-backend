package com.worldcup.hotelbooking.catalog.hotelphoto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HotelPhotoRepository extends JpaRepository<HotelPhoto, Long> {

    List<HotelPhoto> findByHotelIdOrderBySortOrderAscCreatedAtAsc(Long hotelId);

    boolean existsByHotelIdAndId(Long hotelId, Long photoId);
}
