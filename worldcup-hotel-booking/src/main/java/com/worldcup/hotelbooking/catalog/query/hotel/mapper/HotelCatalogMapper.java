package com.worldcup.hotelbooking.catalog.query.hotel.mapper;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import org.springframework.stereotype.Component;

@Component
public class HotelCatalogMapper {

    public HotelCatalogResponseDto toDto(Hotel hotel, String primaryPhotoUrl) {
        return new HotelCatalogResponseDto(
                hotel.getName(),
                hotel.getId(),
                hotel.getDescription(),
                hotel.getCity(),
                hotel.getCountry(),
                primaryPhotoUrl
        );
    }
}