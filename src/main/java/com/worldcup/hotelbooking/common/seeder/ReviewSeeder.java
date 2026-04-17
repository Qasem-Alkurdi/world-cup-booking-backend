package com.worldcup.hotelbooking.common.seeder;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.reservation.booking.Booking;
import com.worldcup.hotelbooking.reservation.booking.BookingRepository;
import com.worldcup.hotelbooking.review.Review;
import com.worldcup.hotelbooking.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(7)
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class ReviewSeeder implements CommandLineRunner {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (reviewRepository.count() > 0) {
            log.info("Reviews already exist. Skipping review seeder.");
            return;
        }

        List<Booking> checkedOutBookings = bookingRepository.findAll().stream()
                .filter(booking -> booking.getStatus() == Booking.BookingStatus.CHECKED_OUT)
                .filter(booking -> booking.getHotel() != null)
                .filter(booking -> booking.getAppUser() != null)
                .sorted(Comparator.comparing(Booking::getId))
                .toList();

        if (checkedOutBookings.isEmpty()) {
            log.warn("No checked-out bookings found. Skipping review seeder.");
            return;
        }

        List<ReviewTemplate> templates = List.of(
                new ReviewTemplate(5, "Excellent stay, very clean rooms and great location near the stadium."),
                new ReviewTemplate(4, "Very good hotel overall, convenient and comfortable."),
                new ReviewTemplate(5, "Amazing experience, friendly staff and smooth check-in/check-out."),
                new ReviewTemplate(3, "Good stay overall but there is room for improvement in service speed."),
                new ReviewTemplate(4, "Nice hotel with solid amenities and a good match-day location."),
                new ReviewTemplate(2, "Location was useful, but the room experience was below expectations."),
                new ReviewTemplate(5, "Outstanding stay, would definitely book again."),
                new ReviewTemplate(4, "Clean and practical hotel, especially for tournament travel."),
                new ReviewTemplate(1, "Poor experience overall, not matching the expected quality."),
                new ReviewTemplate(3, "Average stay, acceptable but not memorable.")
        );

        int reviewsToCreate = Math.min(checkedOutBookings.size(), templates.size());

        for (int i = 0; i < reviewsToCreate; i++) {
            Booking booking = checkedOutBookings.get(i);

            if (reviewRepository.existsByBookingId(booking.getId())) {
                continue;
            }

            ReviewTemplate template = templates.get(i);

            Review review = new Review();
            review.setBooking(booking);
            review.setHotel(booking.getHotel());
            review.setUser(booking.getAppUser());
            review.setRating(template.rating());
            review.setComment(template.comment());
            review.setVisible(true);

            reviewRepository.save(review);
        }

        recalculateAllHotelRatings();

        log.info("Seeded {} reviews successfully.", reviewRepository.count());
    }

    private void recalculateAllHotelRatings() {
        List<Review> visibleReviews = reviewRepository.findAll().stream()
                .filter(Review::isVisible)
                .filter(review -> review.getHotel() != null)
                .toList();

        Map<Long, List<Review>> reviewsByHotelId = visibleReviews.stream()
                .collect(Collectors.groupingBy(review -> review.getHotel().getId()));

        List<Hotel> hotels = hotelRepository.findAll();

        for (Hotel hotel : hotels) {
            List<Review> hotelReviews = reviewsByHotelId.getOrDefault(hotel.getId(), List.of());

            int count = hotelReviews.size();

            if (count == 0) {
                hotel.setReviewCount(0);
                hotel.setAverageRating(BigDecimal.ZERO);
                continue;
            }

            int sum = hotelReviews.stream()
                    .mapToInt(Review::getRating)
                    .sum();

            BigDecimal average = BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

            hotel.setReviewCount(count);
            hotel.setAverageRating(average);
        }

        hotelRepository.saveAll(hotels);
    }

    private record ReviewTemplate(
            int rating,
            String comment
    ) {
    }
}