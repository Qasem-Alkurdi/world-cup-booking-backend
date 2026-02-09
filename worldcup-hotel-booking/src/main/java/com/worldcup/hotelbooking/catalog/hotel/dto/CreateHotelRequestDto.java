package com.worldcup.hotelbooking.catalog.hotel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CreateHotelRequestDto {

    @NotNull
    private final Long ownerId; // مؤقت للتست

    @NotBlank
    private final String name;

    private final String description;

    @Email
    private final String contactEmail;

    private final String contactPhone;

    @NotBlank
    private final String country;

    @NotBlank
    private final String city;

    private final String addressLine;

    @NotNull
    private final Double latitude;

    @NotNull
    private final Double longitude;

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
}
