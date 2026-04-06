package com.worldcup.hotelbooking.catalog.query.roomtype.dto;

import lombok.AllArgsConstructor;

/**
 * Lightweight photo descriptor embedded inside RoomTypeQueryResponseDto.
 */
@AllArgsConstructor
public record RoomTypePhotoDto(Long id, String url, String caption, boolean primary, Integer sortOrder) {
}
