package com.worldcup.hotelbooking.catalog.hotel.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


public record HotelResponseDto(Long id, Long ownerId, String name, String description, String contactEmail,
                               String contactPhone, String country, String city, String addressLine, Double latitude,
                               Double longitude, String status, Boolean hasWifi, Boolean hasParking,
                               Boolean hasBreakfast, Boolean hasAirConditioning, Boolean hasHeating,
                               Boolean hasElevator, Boolean hasRestaurant, Boolean hasRoomService, Boolean hasGym,
                               Boolean hasPool, Boolean hasSpa, Boolean hasLaundry, Boolean hasAirportShuttle,
                               Boolean hasAccessibleFacilities, Boolean petFriendly, BigDecimal averageRating,
                               Integer reviewCount, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

}
