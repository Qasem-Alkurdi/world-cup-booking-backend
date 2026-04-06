package com.worldcup.hotelbooking.catalog.roomtypephoto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomTypePhotoRepository extends JpaRepository<RoomTypePhoto, Long> {

    List<RoomTypePhoto> findByRoomTypeIdOrderBySortOrderAscCreatedAtAsc(Long roomTypeId);

    List<RoomTypePhoto> findByRoomTypeIdInOrderBySortOrderAscCreatedAtAsc(List<Long> roomTypeIds);

    Optional<RoomTypePhoto> findByIdAndRoomTypeId(Long photoId, Long roomTypeId);

    Optional<RoomTypePhoto> findByRoomTypeIdAndPrimaryTrue(Long roomTypeId);

    @Query("""
            select coalesce(max(rtp.sortOrder), 0) + 1
            from RoomTypePhoto rtp
            where rtp.roomType.id = :roomTypeId
            """)
    Integer findNextSortOrderByRoomTypeId(@Param("roomTypeId") Long roomTypeId);
}