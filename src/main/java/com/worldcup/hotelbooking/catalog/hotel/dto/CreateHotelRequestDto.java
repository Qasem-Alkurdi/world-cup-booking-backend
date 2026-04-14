package com.worldcup.hotelbooking.catalog.hotel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateHotelRequestDto(@NotBlank String name, String description, @Email String contactEmail,
                                    String contactPhone, @NotBlank String country, @NotBlank String city,
                                    String addressLine, @NotNull Double latitude, @NotNull Double longitude,
                                    Boolean hasWifi, Boolean hasParking, Boolean hasBreakfast,
                                    Boolean hasAirConditioning, Boolean hasHeating, Boolean hasElevator,
                                    Boolean hasRestaurant, Boolean hasRoomService, Boolean hasGym, Boolean hasPool,
                                    Boolean hasSpa, Boolean hasLaundry, Boolean hasAirportShuttle,
                                    Boolean hasAccessibleFacilities, Boolean petFriendly) {


}
