package com.worldcup.hotelbooking.review;

import com.worldcup.hotelbooking.review.dto.CreateReviewRequestDto;
import com.worldcup.hotelbooking.review.dto.HotelReviewSummaryDto;
import com.worldcup.hotelbooking.review.dto.ReviewResponseDto;
import com.worldcup.hotelbooking.review.dto.UpdateReviewRequestDto;

import java.util.List;

public interface ReviewService {
    ReviewResponseDto create(Long bookingId, CreateReviewRequestDto body);

    ReviewResponseDto update(Long reviewId, UpdateReviewRequestDto body);

    void delete(Long reviewId);

    List<ReviewResponseDto> getHotelReviews(Long hotelId);

    ReviewResponseDto getById(Long reviewId);

    HotelReviewSummaryDto getHotelReviewSummary(Long hotelId);
}