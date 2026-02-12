package com.worldcup.hotelbooking.catalog.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@AllArgsConstructor
@Getter
public class HotelResponseDto {

    private final Long id;
    private final Long ownerId;

    private final String name;
    private final String description;

    private final String contactEmail;
    private final String contactPhone;

    private final String country;
    private final String city;
    private final String addressLine;

    private final Double latitude;
    private final Double longitude;

    private final String status;

    private final Boolean hasWifi;
    private final Boolean hasParking;
    private final Boolean hasBreakfast;
    private final Boolean hasAirConditioning;
    private final Boolean hasHeating;
    private final Boolean hasElevator;
    private final Boolean hasRestaurant;
    private final Boolean hasRoomService;
    private final Boolean hasGym;
    private final Boolean hasPool;
    private final Boolean hasSpa;
    private final Boolean hasLaundry;
    private final Boolean hasAirportShuttle;
    private final Boolean hasAccessibleFacilities;
    private final Boolean petFriendly;

    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
}
