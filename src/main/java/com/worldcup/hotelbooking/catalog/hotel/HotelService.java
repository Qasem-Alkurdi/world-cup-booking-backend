package com.worldcup.hotelbooking.catalog.hotel;

import com.worldcup.hotelbooking.catalog.hotel.dto.UpdateHotelPatchRequest;

import java.util.List;

public interface HotelService {
    List<Hotel> findAll();

    Hotel findById(Long id);

    Hotel create(Hotel hotel, Long ownerId);

    Hotel replace(Long id, Hotel hotel);

    Hotel updatePartial(Long id, UpdateHotelPatchRequest dto);

    void deleteById(Long id);

    List<Hotel> getMyHotels(Long ownerId);
}
