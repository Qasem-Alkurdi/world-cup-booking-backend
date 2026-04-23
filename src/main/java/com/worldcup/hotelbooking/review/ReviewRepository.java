package com.worldcup.hotelbooking.review;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<Review> findByHotelAndVisibleTrueOrderByCreatedAtDesc(Hotel hotel);

    @EntityGraph(attributePaths = {"user"})
    List<Review> findByHotelOrderByCreatedAtDesc(Hotel hotel);

    @EntityGraph(attributePaths = {"user", "hotel", "booking"})
    Optional<Review> findDetailedById(Long id);

    Optional<Review> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    @Query("""
                select (count(r) > 0)
                from Review r
                where r.id = :reviewId
                  and r.user.username = :username
            """)
    boolean existsManageableReview(@Param("reviewId") Long reviewId,
                                   @Param("username") String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Review r WHERE r.booking.id IN :bookingIds")
    void deleteByBookingIdIn(@Param("bookingIds") List<Long> bookingIds);
}