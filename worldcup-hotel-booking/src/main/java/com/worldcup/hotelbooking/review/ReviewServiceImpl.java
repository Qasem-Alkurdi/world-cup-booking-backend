package com.worldcup.hotelbooking.review;

import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.booking.booking.BookingRepository;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotel.exception.HotelNotFoundException;
import com.worldcup.hotelbooking.review.dto.CreateReviewRequestDto;
import com.worldcup.hotelbooking.review.dto.HotelReviewSummaryDto;
import com.worldcup.hotelbooking.review.dto.UpdateReviewRequestDto;
import com.worldcup.hotelbooking.review.exception.ReviewAlreadyExistsException;
import com.worldcup.hotelbooking.review.exception.ReviewNotAllowedException;
import com.worldcup.hotelbooking.review.exception.ReviewNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.worldcup.hotelbooking.booking.booking.Booking.BookingStatus.CHECKED_OUT;
import static com.worldcup.hotelbooking.catalog.hotel.HotelStatus.APPROVED;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;

    public ReviewServiceImpl(
            ReviewRepository reviewRepository,
            BookingRepository bookingRepository,
            HotelRepository hotelRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.hotelRepository = hotelRepository;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "hotelById", allEntries = true),
            @CacheEvict(value = "hotelList", allEntries = true),
            @CacheEvict(value = "myHotels", allEntries = true)
    })
    public Review create(Long bookingId, CreateReviewRequestDto body) {
        if (reviewRepository.existsByBookingId(bookingId)) {
            throw new ReviewAlreadyExistsException(bookingId);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ReviewNotAllowedException("Booking not found: " + bookingId));

        validateReviewCreation(booking);

        Hotel hotel = booking.getHotel();

        Review review = new Review();
        review.setBooking(booking);
        review.setUser(booking.getAppUser());
        review.setHotel(hotel);
        review.setRating(body.rating());
        review.setComment(body.comment());
        review.setVisible(true);

        Review saved = reviewRepository.save(review);
        recalculateHotelRating(hotel);

        return saved;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "hotelById", allEntries = true),
            @CacheEvict(value = "hotelList", allEntries = true),
            @CacheEvict(value = "myHotels", allEntries = true)
    })
    public Review update(Long reviewId, UpdateReviewRequestDto body) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (body.getRating() != null) {
            review.setRating(body.getRating());
        }

        if (body.getComment() != null) {
            review.setComment(body.getComment());
        }

        recalculateHotelRating(review.getHotel());
        return review;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "hotelById", allEntries = true),
            @CacheEvict(value = "hotelList", allEntries = true),
            @CacheEvict(value = "myHotels", allEntries = true)
    })
    public void delete(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        Hotel hotel = review.getHotel();
        reviewRepository.delete(review);
        recalculateHotelRating(hotel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> getHotelReviews(Long hotelId) {
        Hotel hotel = hotelRepository.findByIdAndStatusAndIsDeletedFalse(hotelId, APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        return reviewRepository.findByHotelAndVisibleTrueOrderByCreatedAtDesc(hotel);
    }

    @Override
    @Transactional(readOnly = true)
    public Review getById(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public HotelReviewSummaryDto getHotelReviewSummary(Long hotelId) {
        Hotel hotel = hotelRepository.findByIdAndStatusAndIsDeletedFalse(hotelId, APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        return new HotelReviewSummaryDto(
                hotel.getId(),
                hotel.getAverageRating() == null ? BigDecimal.ZERO : hotel.getAverageRating(),
                hotel.getReviewCount()
        );
    }

    private void validateReviewCreation(Booking booking) {
        if (booking.getAppUser() == null) {
            throw new ReviewNotAllowedException("Booking is not linked to a user.");
        }

        if (booking.getHotel() == null) {
            throw new ReviewNotAllowedException("Booking is not linked to a hotel.");
        }

        if (booking.getStatus() != CHECKED_OUT) {
            throw new ReviewNotAllowedException("Review is allowed only after check-out.");
        }

        if (booking.getHotel().isDeleted() || booking.getHotel().getStatus() != APPROVED) {
            throw new ReviewNotAllowedException("Hotel is not available for reviews.");
        }
    }

    private void recalculateHotelRating(Hotel hotel) {
        List<Review> visibleReviews = reviewRepository.findByHotelAndVisibleTrueOrderByCreatedAtDesc(hotel);

        int count = visibleReviews.size();

        if (count == 0) {
            hotel.setReviewCount(0);
            hotel.setAverageRating(BigDecimal.ZERO);
            return;
        }

        int sum = visibleReviews.stream()
                .mapToInt(Review::getRating)
                .sum();

        BigDecimal average = BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        hotel.setReviewCount(count);
        hotel.setAverageRating(average);
    }
}