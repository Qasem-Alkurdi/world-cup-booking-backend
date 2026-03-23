package com.worldcup.hotelbooking.review;

import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.booking.booking.BookingRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ReviewAuthorizationService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public ReviewAuthorizationService(ReviewRepository reviewRepository,
                                      BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
    }

    public boolean canCreateReview(Long bookingId, Authentication authentication) {
        String username = authentication.getName();

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || booking.getAppUser() == null) {
            return false;
        }

        return username.equals(booking.getAppUser().getUsername());
    }

    public boolean canManageReview(Long reviewId, Authentication authentication) {
        String username = authentication.getName();

        return reviewRepository.findById(reviewId)
                .map(review -> review.getUser() != null
                        && username.equals(review.getUser().getUsername()))
                .orElse(false);
    }
}