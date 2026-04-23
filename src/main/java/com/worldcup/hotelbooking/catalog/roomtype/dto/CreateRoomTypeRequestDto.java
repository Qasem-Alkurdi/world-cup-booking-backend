package com.worldcup.hotelbooking.catalog.roomtype.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * @param roomSizeSqm        room_size_sqm can be null, but if provided must be > 0
 * @param currency           optional; default handled in entity/mapper ("USD")
 * @param hasPrivateBathroom amenities (optional in create)
 */

public record CreateRoomTypeRequestDto(@NotBlank String name, String description, @NotNull @Min(0) Integer maxAdults,
                                       @NotNull @Min(0) Integer maxChildren,
                                       @DecimalMin(value = "0.01") BigDecimal roomSizeSqm,
                                       @NotNull @DecimalMin(value = "0.00") BigDecimal basePrice, String currency,
                                       @NotNull @Min(0) Integer totalRooms, Boolean hasPrivateBathroom,
                                       Boolean hasAirConditioning, Boolean hasHeating, Boolean hasBalcony,
                                       Boolean hasTv, Boolean hasMinibar, Boolean hasSafe, Boolean hasHairdryer,
                                       Boolean hasWorkDesk, Boolean hasSoundproofing, Boolean hasCoffeeMachine) {

}
