package com.worldcup.hotelbooking.catalog.roomtypephoto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomTypePhotoRepository extends JpaRepository<RoomTypePhoto, Long> {

    List<RoomTypePhoto> findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(Long roomTypeId);

    boolean existsByRoomTypeIdAndId(Long roomTypeId, Long photoId);
}
