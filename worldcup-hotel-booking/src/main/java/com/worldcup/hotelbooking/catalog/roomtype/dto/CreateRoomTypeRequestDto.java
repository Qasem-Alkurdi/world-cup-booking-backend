package com.worldcup.hotelbooking.catalog.roomtype.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
public class CreateRoomTypeRequestDto {

    @NotBlank
    private final String name;

    private final String description;

    @NotNull
    @Min(0)
    private final Integer maxAdults;

    @NotNull
    @Min(0)
    private final Integer maxChildren;

    // room_size_sqm can be null, but if provided must be > 0
    @DecimalMin(value = "0.01")
    private final BigDecimal roomSizeSqm;

    @NotNull
    @DecimalMin(value = "0.00")
    private final BigDecimal basePrice;

    // optional; default handled in entity/mapper ("USD")
    private final String currency;

    @NotNull
    @Min(0)
    private final Integer totalRooms;

    // amenities (optional in create)
    private final Boolean hasPrivateBathroom;
    private final Boolean hasAirConditioning;
    private final Boolean hasHeating;
    private final Boolean hasBalcony;
    private final Boolean hasTv;
    private final Boolean hasMinibar;
    private final Boolean hasSafe;
    private final Boolean hasHairdryer;
    private final Boolean hasWorkDesk;
    private final Boolean hasSoundproofing;
    private final Boolean hasCoffeeMachine;
}
