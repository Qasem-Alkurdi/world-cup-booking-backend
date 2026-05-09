package com.worldcup.hotelbooking.catalog.print.dto;

import java.math.BigDecimal;
import java.util.List;

public record HotelCatalogCompositeResponseDto(
        Long id,
        String name,
        String description,
        String contactEmail,
        String contactPhone,
        String country,
        String city,
        String addressLine,
        BigDecimal averageRating,
        Integer reviewCount,
        boolean hasWifi,
        boolean hasParking,
        boolean hasBreakfast,
        boolean hasAirConditioning,
        boolean hasHeating,
        boolean hasElevator,
        boolean hasRestaurant,
        boolean hasRoomService,
        boolean hasGym,
        boolean hasPool,
        boolean hasSpa,
        boolean hasLaundry,
        boolean hasAirportShuttle,
        boolean hasAccessibleFacilities,
        boolean petFriendly,
        List<RoomTypeCatalogLeafResponseDto> roomTypes
) {
}