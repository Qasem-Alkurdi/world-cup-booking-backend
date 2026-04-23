package com.worldcup.hotelbooking.catalog.roomtype.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * @param hasPrivateBathroom amenities (PUT => required to avoid accidental resets)
 */

public record ReplaceRoomTypeRequestDto(@NotBlank String name, String description, @NotNull @Min(0) Integer maxAdults,
                                        @NotNull @Min(0) Integer maxChildren,
                                        @DecimalMin(value = "0.01") BigDecimal roomSizeSqm,
                                        @NotNull @DecimalMin(value = "0.00") BigDecimal basePrice,
                                        @NotBlank String currency, @NotNull @Min(0) Integer totalRooms,
                                        @NotNull Boolean hasPrivateBathroom, @NotNull Boolean hasAirConditioning,
                                        @NotNull Boolean hasHeating, @NotNull Boolean hasBalcony,
                                        @NotNull Boolean hasTv, @NotNull Boolean hasMinibar, @NotNull Boolean hasSafe,
                                        @NotNull Boolean hasHairdryer, @NotNull Boolean hasWorkDesk,
                                        @NotNull Boolean hasSoundproofing, @NotNull Boolean hasCoffeeMachine) {

}
