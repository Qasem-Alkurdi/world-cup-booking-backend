package com.worldcup.hotelbooking.catalog.hotel;

import java.util.List;

public interface HotelServiceInterface {
    List<Hotel> findAll();
    Hotel findById(Long id);
    Hotel create(Hotel hotel);
    Hotel replace(Long id,Hotel hotel);
    void deleteById(Long id);
}
