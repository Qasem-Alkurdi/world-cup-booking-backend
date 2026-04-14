package com.worldcup.hotelbooking.catalog.hotelphoto.dto;

import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

/**
 * @param url resolved from storageKey
 */
@AllArgsConstructor
public record HotelPhotoResponseDto(Long id, String url, String caption, Integer sortOrder, OffsetDateTime createdAt) {
}
