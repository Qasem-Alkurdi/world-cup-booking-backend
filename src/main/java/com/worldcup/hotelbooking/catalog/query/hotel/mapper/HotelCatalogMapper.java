package com.worldcup.hotelbooking.catalog.query.hotel.mapper;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HotelCatalogMapper {

    public HotelCatalogResponseDto toDto(Hotel hotel, String primaryPhotoUrl, BigDecimal minPrice, Double distanceKm) {
        return new HotelCatalogResponseDto(
                hotel.getName(),
                hotel.getId(),
                hotel.getDescription(),
                hotel.getCity(),
                hotel.getCountry(),
                primaryPhotoUrl,
                hotel.getAverageRating(),
                hotel.getReviewCount(),
                minPrice,
                distanceKm,
                hotel.isHasGym(),
                hotel.isHasWifi(),
                hotel.isHasParking(),
                hotel.isHasBreakfast(),
                hotel.isHasAirConditioning(),
                hotel.isHasHeating(),
                hotel.isHasPool(),
                hotel.isHasSpa(),
                hotel.isHasElevator(),
                hotel.isHasRestaurant(),
                hotel.isHasRoomService(),
                hotel.isHasLaundry(),
                hotel.isHasAirportShuttle(),
                hotel.isHasAccessibleFacilities(),
                hotel.isPetFriendly()
        );
    }
}