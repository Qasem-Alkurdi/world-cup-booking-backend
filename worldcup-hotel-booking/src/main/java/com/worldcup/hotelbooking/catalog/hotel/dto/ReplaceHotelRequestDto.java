package com.worldcup.hotelbooking.catalog.hotel.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ReplaceHotelRequestDto {

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
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private final Double latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private final Double longitude;

    @NotNull
    private final Boolean hasWifi;
    @NotNull
    private final Boolean hasParking;
    @NotNull
    private final Boolean hasBreakfast;
    @NotNull
    private final Boolean hasAirConditioning;
    @NotNull
    private final Boolean hasHeating;
    @NotNull
    private final Boolean hasElevator;
    @NotNull
    private final Boolean hasRestaurant;
    @NotNull
    private final Boolean hasRoomService;
    @NotNull
    private final Boolean hasGym;
    @NotNull
    private final Boolean hasPool;
    @NotNull
    private final Boolean hasSpa;
    @NotNull
    private final Boolean hasLaundry;
    @NotNull
    private final Boolean hasAirportShuttle;
    @NotNull
    private final Boolean hasAccessibleFacilities;
    @NotNull
    private final Boolean petFriendly;
}
