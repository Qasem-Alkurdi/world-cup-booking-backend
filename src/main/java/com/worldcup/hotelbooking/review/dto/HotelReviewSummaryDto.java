package com.worldcup.hotelbooking.review.dto;

import java.math.BigDecimal;

public record HotelReviewSummaryDto(
        Long hotelId,
        BigDecimal averageRating,
        int reviewCount
) {
}