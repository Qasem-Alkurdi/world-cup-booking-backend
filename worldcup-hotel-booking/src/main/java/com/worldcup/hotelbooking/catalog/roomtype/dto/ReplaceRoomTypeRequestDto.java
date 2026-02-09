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
public class ReplaceRoomTypeRequestDto {

    @NotBlank
    private final String name;

    private final String description;

    @NotNull
    @Min(0)
    private final Integer maxAdults;

    @NotNull
    @Min(0)
    private final Integer maxChildren;

    @DecimalMin(value = "0.01")
    private final BigDecimal roomSizeSqm;

    @NotNull
    @DecimalMin(value = "0.00")
    private final BigDecimal basePrice;

    @NotBlank
    private final String currency;

    @NotNull
    @Min(0)
    private final Integer totalRooms;

    // amenities (PUT => required to avoid accidental resets)
    @NotNull
    private final Boolean hasPrivateBathroom;
    @NotNull
    private final Boolean hasAirConditioning;
    @NotNull
    private final Boolean hasHeating;
    @NotNull
    private final Boolean hasBalcony;
    @NotNull
    private final Boolean hasTv;
    @NotNull
    private final Boolean hasMinibar;
    @NotNull
    private final Boolean hasSafe;
    @NotNull
    private final Boolean hasHairdryer;
    @NotNull
    private final Boolean hasWorkDesk;
    @NotNull
    private final Boolean hasSoundproofing;
    @NotNull
    private final Boolean hasCoffeeMachine;
}
