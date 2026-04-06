package com.worldcup.hotelbooking.catalog.query.roomtype.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Read-optimised DTO for room-type catalog queries.
 * Contains the full amenity list and a resolved primary-photo URL
 * so the frontend never needs a second round-trip.
 *
 * @param name               ── Core ───────────────────────────────────────────
 * @param maxAdults          ── Capacity ───────────────────────────────────────
 * @param roomSizeSqm        ── Dimensions & pricing ───────────────────────────
 * @param totalPrice         Total for the stay (if dates selected)
 * @param nightlyPrice       Dynamic average p/n (if dates selected)
 * @param priceExplanation   e.g. "Includes Finals week premium"
 * @param totalRooms         ── Availability ───────────────────────────────────
 * @param hasPrivateBathroom ── Room amenities ─────────────────────────────────
 * @param primaryPhotoUrl    ── Photos (resolved URLs) ─────────────────────────
 */

public record RoomTypeQueryResponseDto(Long id, Long hotelId, String name, String description, Integer maxAdults,
                                       Integer maxChildren, BigDecimal roomSizeSqm, BigDecimal basePrice,
                                       String currency, BigDecimal totalPrice, BigDecimal nightlyPrice,
                                       String priceExplanation, Integer totalRooms, boolean hasPrivateBathroom,
                                       boolean hasAirConditioning, boolean hasHeating, boolean hasBalcony,
                                       boolean hasTv, boolean hasMinibar, boolean hasSafe, boolean hasHairdryer,
                                       boolean hasWorkDesk, boolean hasSoundproofing, boolean hasCoffeeMachine,
                                       String primaryPhotoUrl, List<RoomTypePhotoDto> photos) {

}
