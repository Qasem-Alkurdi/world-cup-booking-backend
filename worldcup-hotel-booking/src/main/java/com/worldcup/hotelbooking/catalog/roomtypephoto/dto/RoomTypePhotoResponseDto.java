package com.worldcup.hotelbooking.catalog.roomtypephoto.dto;

import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

@AllArgsConstructor
public record RoomTypePhotoResponseDto(Long id, String url, String caption, Integer sortOrder,
                                       OffsetDateTime createdAt) {
}