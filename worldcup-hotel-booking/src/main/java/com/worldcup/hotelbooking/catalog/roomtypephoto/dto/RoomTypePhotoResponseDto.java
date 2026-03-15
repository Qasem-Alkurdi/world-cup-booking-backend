package com.worldcup.hotelbooking.catalog.roomtypephoto.dto;

import java.time.OffsetDateTime;

public record RoomTypePhotoResponseDto(
        Long id,
        String url,
        String caption,
        Integer sortOrder,
        OffsetDateTime createdAt
) {
}