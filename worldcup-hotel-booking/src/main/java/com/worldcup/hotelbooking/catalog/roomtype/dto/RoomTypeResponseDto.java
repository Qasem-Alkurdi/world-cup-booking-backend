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

    private final Integer maxAdults;
    private final Integer maxChildren;

    private final BigDecimal roomSizeSqm;

    private final BigDecimal basePrice;
    private final String currency;

    private final Integer totalRooms;

    private final boolean hasPrivateBathroom;
    private final boolean hasAirConditioning;
    private final boolean hasHeating;
    private final boolean hasBalcony;
    private final boolean hasTv;
    private final boolean hasMinibar;
    private final boolean hasSafe;
    private final boolean hasHairdryer;
    private final boolean hasWorkDesk;
    private final boolean hasSoundproofing;
    private final boolean hasCoffeeMachine;

    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
}
