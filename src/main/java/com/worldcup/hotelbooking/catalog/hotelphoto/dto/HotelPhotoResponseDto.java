package com.worldcup.hotelbooking.catalog.hotelphoto.dto;

import java.time.OffsetDateTime;

/**
 * @param url resolved from storageKey
 */

public record HotelPhotoResponseDto(Long id, String url, String caption, Integer sortOrder, OffsetDateTime createdAt) {
}
