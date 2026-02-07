package com.worldcup.hotelbooking.catalog.hotel;

import org.springframework.stereotype.Service;

@Service
public class HotelService {
    private final HotelRepository hotelRepository;
    HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }
    public Hotel getHotelById(Long id) {
        return hotelRepository.findById(id).orElseThrow(() -> new HotelNotFoundException("Hotel not found with id: " + id));
    }
}
