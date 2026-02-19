package com.worldcup.hotelbooking.catalog.hotel;

import com.worldcup.hotelbooking.user.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface HotelRepository extends JpaRepository<Hotel, Long>, JpaSpecificationExecutor<Hotel> {

    List<Hotel> findByStatusAndIsDeletedFalse(HotelStatus status);

    Optional<Hotel> findByIdAndStatusAndIsDeletedFalse(Long id, HotelStatus status);

    List<Hotel> findByOwnerAndStatusAndIsDeletedFalse(AppUser owner, HotelStatus status);
}
