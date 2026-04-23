package com.worldcup.hotelbooking.catalog.hotel.dto;

import jakarta.validation.constraints.*;


public record ReplaceHotelRequestDto(@NotBlank String name, String description, @Email String contactEmail,
                                     String contactPhone, @NotBlank String country, @NotBlank String city,
                                     String addressLine,
                                     @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
                                     @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
                                     @NotNull Boolean hasWifi, @NotNull Boolean hasParking,
                                     @NotNull Boolean hasBreakfast, @NotNull Boolean hasAirConditioning,
                                     @NotNull Boolean hasHeating, @NotNull Boolean hasElevator,
                                     @NotNull Boolean hasRestaurant, @NotNull Boolean hasRoomService,
                                     @NotNull Boolean hasGym, @NotNull Boolean hasPool, @NotNull Boolean hasSpa,
                                     @NotNull Boolean hasLaundry, @NotNull Boolean hasAirportShuttle,
                                     @NotNull Boolean hasAccessibleFacilities, @NotNull Boolean petFriendly) {

}
