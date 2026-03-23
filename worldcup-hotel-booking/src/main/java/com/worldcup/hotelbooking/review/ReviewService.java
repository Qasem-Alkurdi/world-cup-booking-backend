package com.worldcup.hotelbooking.review;

import com.worldcup.hotelbooking.review.dto.CreateReviewRequestDto;
import com.worldcup.hotelbooking.review.dto.HotelReviewSummaryDto;
import com.worldcup.hotelbooking.review.dto.UpdateReviewRequestDto;

import java.util.List;

public interface ReviewService {
    Review create(Long bookingId, CreateReviewRequestDto body);

    Review update(Long reviewId, UpdateReviewRequestDto body);

    void delete(Long reviewId);

    List<Review> getHotelReviews(Long hotelId);

    Review getById(Long reviewId);

    HotelReviewSummaryDto getHotelReviewSummary(Long hotelId);
}