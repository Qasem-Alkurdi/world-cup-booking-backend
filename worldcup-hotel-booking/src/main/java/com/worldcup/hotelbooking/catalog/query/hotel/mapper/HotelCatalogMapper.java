package com.worldcup.hotelbooking.catalog.query.hotel.mapper;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HotelCatalogMapper {

    public HotelCatalogResponseDto toDto(
            Hotel hotel,
            String primaryPhotoUrl,
            BigDecimal startingPrice,
            Double distanceKm
    ) {
        return new HotelCatalogResponseDto(
                hotel.getId(),
                hotel.getName(),
                hotel.getDescription(),
                hotel.getCity(),
                hotel.getCountry(),
                primaryPhotoUrl,
                startingPrice,
                hotel.getAverageRating(),
                hotel.getReviewCount(),
                distanceKm
        );
    }
}