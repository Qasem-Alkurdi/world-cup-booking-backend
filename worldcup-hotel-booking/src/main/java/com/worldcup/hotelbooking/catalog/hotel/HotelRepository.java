package com.worldcup.hotelbooking.catalog.hotel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HotelRepository extends JpaRepository<Hotel,Long> {
    List<Hotel> findByStatus(HotelStatus status);
    Optional<Hotel> findByIdAndStatus(Long id, HotelStatus status);

}
