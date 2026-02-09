package com.worldcup.hotelbooking.catalog.roomtype.dto;

import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@AllArgsConstructor
public record RoomTypeResponseDto(Long id, Long hotelId, String name, String description, Integer maxGuests,
                                  BigDecimal roomSizeSqm, BigDecimal basePrice, String currency, Integer totalRooms,
                                  Boolean hasPrivateBathroom, Boolean hasAirConditioning, Boolean hasHeating,
                                  Boolean hasBalcony, Boolean hasTv, Boolean hasMinibar, Boolean hasSafe,
                                  Boolean hasHairdryer, Boolean hasWorkDesk, Boolean hasSoundproofing,
                                  Boolean hasCoffeeMachine, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

}
