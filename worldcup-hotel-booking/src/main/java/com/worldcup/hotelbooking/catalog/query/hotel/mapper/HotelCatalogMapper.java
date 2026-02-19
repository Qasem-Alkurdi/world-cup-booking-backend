package com.worldcup.hotelbooking.catalog.query.hotel.mapper;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;

public class HotelCatalogMapper {
    public static HotelCatalogResponseDto toDto(Hotel hotel) {
        return new HotelCatalogResponseDto(
                hotel.getName(),
                hotel.getId(),
                hotel.getDescription(),
                hotel.getCity(),
                hotel.getCountry()
        );
    }
}
