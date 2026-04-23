package com.worldcup.hotelbooking.catalog.roomtype.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


public record RoomTypeResponseDto(Long id, Long hotelId, String name, String description, Integer maxAdults,
                                  Integer maxChildren, BigDecimal roomSizeSqm, BigDecimal basePrice, String currency,
                                  Integer totalRooms, boolean hasPrivateBathroom, boolean hasAirConditioning,
                                  boolean hasHeating, boolean hasBalcony, boolean hasTv, boolean hasMinibar,
                                  boolean hasSafe, boolean hasHairdryer, boolean hasWorkDesk, boolean hasSoundproofing,
                                  boolean hasCoffeeMachine, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

}
