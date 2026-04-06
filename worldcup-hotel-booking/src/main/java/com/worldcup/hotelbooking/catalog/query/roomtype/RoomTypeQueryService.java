package com.worldcup.hotelbooking.catalog.query.roomtype;

import com.worldcup.hotelbooking.catalog.query.roomtype.dto.RoomTypeQueryResponseDto;
import com.worldcup.hotelbooking.catalog.roomtype.dto.RoomTypeAvailabilityCriteria;

import java.util.List;

/**
 * Read-only service for querying room types in the catalog.
 * Separated from the write-side (RoomTypeService) following CQRS principles.
 */
public interface RoomTypeQueryService {

    /**
     * Returns all available room types for a hotel, with resolved photo URLs
     * and optional availability/capacity filtering.
     *
     * @param hotelId  the hotel to query
     * @param criteria optional filters (dates, adults, children, rooms)
     * @return ordered list of room type query DTOs
     */
    List<RoomTypeQueryResponseDto> findAvailableByHotel(Long hotelId, RoomTypeAvailabilityCriteria criteria);
}
