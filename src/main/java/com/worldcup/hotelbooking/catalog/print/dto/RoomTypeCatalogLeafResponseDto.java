package com.worldcup.hotelbooking.catalog.print.dto;

import java.math.BigDecimal;

public record RoomTypeCatalogLeafResponseDto(
        Long id,
        String name,
        String description,
        Integer maxAdults,
        Integer maxChildren,
        BigDecimal roomSizeSqm,
        BigDecimal basePrice,
        String currency,
        Integer totalRooms,
        boolean hasPrivateBathroom,
        boolean hasAirConditioning,
        boolean hasHeating,
        boolean hasBalcony,
        boolean hasTv,
        boolean hasMinibar,
        boolean hasSafe,
        boolean hasHairdryer,
        boolean hasWorkDesk,
        boolean hasSoundproofing,
        boolean hasCoffeeMachine
) {
}