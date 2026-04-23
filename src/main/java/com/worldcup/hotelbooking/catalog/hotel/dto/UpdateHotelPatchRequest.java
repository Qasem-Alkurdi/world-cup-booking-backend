package com.worldcup.hotelbooking.catalog.hotel.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class UpdateHotelPatchRequest {

    private String name;
    private String description;

    @Email
    private String contactEmail;

    private String contactPhone;
    private String addressLine;

    private Boolean hasWifi;
    private Boolean hasParking;
    private Boolean hasBreakfast;
    private Boolean hasAirConditioning;
    private Boolean hasHeating;
    private Boolean hasElevator;
    private Boolean hasRestaurant;
    private Boolean hasRoomService;
    private Boolean hasGym;
    private Boolean hasPool;
    private Boolean hasSpa;
    private Boolean hasLaundry;
    private Boolean hasAirportShuttle;
    private Boolean hasAccessibleFacilities;
    private Boolean petFriendly;
}
