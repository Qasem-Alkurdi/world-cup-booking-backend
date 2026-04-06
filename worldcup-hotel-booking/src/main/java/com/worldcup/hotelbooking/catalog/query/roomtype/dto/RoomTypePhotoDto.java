package com.worldcup.hotelbooking.catalog.query.roomtype.dto;

/**
 * Lightweight photo descriptor embedded inside RoomTypeQueryResponseDto.
 */

public record RoomTypePhotoDto(Long id, String url, String caption, boolean primary, Integer sortOrder) {
}
