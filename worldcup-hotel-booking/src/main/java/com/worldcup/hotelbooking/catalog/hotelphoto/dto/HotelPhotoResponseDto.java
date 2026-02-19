package com.worldcup.hotelbooking.catalog.hotelphoto.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@AllArgsConstructor
@Getter
public class HotelPhotoResponseDto {
    private final Long id;
    private final String url;        // resolved from storageKey
    private final String caption;
    private final Integer sortOrder;
    private final OffsetDateTime createdAt;
}
