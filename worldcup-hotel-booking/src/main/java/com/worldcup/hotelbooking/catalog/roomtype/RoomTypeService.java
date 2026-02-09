package com.worldcup.hotelbooking.catalog.roomtype;

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
    RoomType replace(Long hotelId, Long roomTypeId, RoomType roomType);

    /**
     * Delete a room type
     */
    void delete(Long hotelId, Long roomTypeId);
}
