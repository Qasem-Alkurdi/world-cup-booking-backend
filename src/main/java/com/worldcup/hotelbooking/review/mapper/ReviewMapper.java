package com.worldcup.hotelbooking.review.mapper;

import com.worldcup.hotelbooking.review.Review;
import com.worldcup.hotelbooking.review.dto.ReviewResponseDto;

public class ReviewMapper {

    private ReviewMapper() {
    }

    public static ReviewResponseDto toResponse(Review review) {
        return new ReviewResponseDto(
                review.getId(),
                review.getHotel().getId(),
                review.getUser().getId(),
                review.getUser().getUsername(),
                review.getBooking().getId(),
                review.getRating(),
                review.getComment(),
                review.isVisible(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}