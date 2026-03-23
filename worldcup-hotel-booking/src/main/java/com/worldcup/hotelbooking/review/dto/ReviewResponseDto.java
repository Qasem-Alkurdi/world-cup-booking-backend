package com.worldcup.hotelbooking.review.dto;

import java.time.OffsetDateTime;


public record ReviewResponseDto(Long id, Long hotelId, Long userId, Long bookingId, Integer rating, String comment,
                                boolean visible, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

}