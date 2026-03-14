package com.worldcup.hotelbooking.catalog.roomtype;

import com.worldcup.hotelbooking.catalog.roomtype.dto.ReplaceRoomTypeRequestDto;
import com.worldcup.hotelbooking.catalog.roomtype.dto.RoomTypeAvailabilityCriteria;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RoomTypeService {

    /**
     * Get all room types for a specific hotel (hotel must not be deleted)
     */
    List<RoomType> findByHotel(Long hotelId);

    /**
     * Get one room type by id within a hotel
     */
    RoomType findById(Long hotelId, Long roomTypeId);

    /**
     * Create a new room type for a hotel
     */
    RoomType create(Long hotelId, RoomType roomType);

    /**
     * Replace (PUT) a room type completely
     */


    @Transactional
    RoomType replace(Long hotelId, Long roomTypeId, ReplaceRoomTypeRequestDto dto);

    /**
     * Delete a room type
     */
    void delete(Long hotelId, Long roomTypeId);

    List<RoomType> findAvailableByHotel(Long hotelId, RoomTypeAvailabilityCriteria criteria);
}
