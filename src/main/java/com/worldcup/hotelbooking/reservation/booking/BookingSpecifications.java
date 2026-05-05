package com.worldcup.hotelbooking.reservation.booking;

import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class BookingSpecifications {

    /**
     * Fetches bookingRooms and roomType eagerly to prevent LazyInitializationException
     */
    public static Specification<Booking> fetchBookingRoomsAndRoomType() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("bookingRooms", JoinType.LEFT).fetch("roomType", JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }
    public static Specification<Booking> fetchAssociations() {
        return (root, query, cb) -> {
            // Only apply fetch joins on the main (non-count) query
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("appUser", JoinType.LEFT);
                root.fetch("hotel", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }
    public static Specification<Booking> hasUserName(String userName) {
        return (root, query, cb) -> {
            // Depending on how user name is stored, e.g., if it's on AppUser
            return cb.like(cb.lower(root.get("appUser").get("username")), "%" + userName.toLowerCase() + "%");
        };
    }

    public static Specification<Booking> hasHotelName(String hotelName) {
        return (root, query, cb) -> {
            return cb.like(cb.lower(root.get("hotel").get("name")), "%" + hotelName.toLowerCase() + "%");
        };
    }


    public static Specification<Booking> hasUser(Long userId) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class)// Only fetch bookingRooms and roomType if we're not doing a count query
            {
                root.fetch("bookingRooms", JoinType.LEFT).fetch("roomType", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.equal(root.get("appUser").get("id"), userId);
        };
    }

    public static Specification<Booking> hasHotel(Long hotelId) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("bookingRooms", JoinType.LEFT).fetch("roomType", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.equal(root.get("hotel").get("id"), hotelId);
        };
    }

    public static Specification<Booking> isPast() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("bookingRooms", JoinType.LEFT).fetch("roomType", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.lessThan(root.get("checkOutDate"), LocalDate.now());
        };
    }

    public static Specification<Booking> isUpcoming() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("bookingRooms", JoinType.LEFT).fetch("roomType", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.greaterThanOrEqualTo(root.get("checkInDate"), LocalDate.now());
        };
    }


    public static Specification<Booking> hasStatus(Booking.BookingStatus status) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("bookingRooms", JoinType.LEFT).fetch("roomType", JoinType.LEFT);
                query.distinct(true);
            }
            return status == null ? null : cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Booking> checkInAfter(LocalDate fromDate) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("bookingRooms", JoinType.LEFT).fetch("roomType", JoinType.LEFT);
                query.distinct(true);
            }
            return fromDate == null ? null : cb.greaterThanOrEqualTo(root.get("checkInDate"), fromDate);
        };
    }

    public static Specification<Booking> checkOutBefore(LocalDate toDate) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("bookingRooms", JoinType.LEFT).fetch("roomType", JoinType.LEFT);
                query.distinct(true);
            }
            return toDate == null ? null : cb.lessThanOrEqualTo(root.get("checkOutDate"), toDate);
        };
    }

    public static Specification<Booking> priceBetween(Double min, Double max) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("bookingRooms", JoinType.LEFT).fetch("roomType", JoinType.LEFT);
                query.distinct(true);
            }
            if (min == null && max == null) return null;
            if (min != null && max != null)
                return cb.between(root.get("totalPrice"), min, max);
            if (min != null)
                return cb.greaterThanOrEqualTo(root.get("totalPrice"), min);
            return cb.lessThanOrEqualTo(root.get("totalPrice"), max);
        };
    }

}
