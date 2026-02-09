package com.worldcup.hotelbooking.catalog.roomtype.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@AllArgsConstructor
@Getter
public class RoomTypeResponseDto {

    private final Long id;
    private final Long hotelId;

    private final String name;
    private final String description;

    private final Integer maxGuests;
    private final BigDecimal roomSizeSqm;

    private final BigDecimal basePrice;
    private final String currency;

    private final Integer totalRooms;

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

    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
}
