package com.worldcup.hotelbooking.booking.booking;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class BookingSpecifications {

    public static Specification<Booking> hasUser(Long userId) {
        return (root, query, cb) ->
                cb.equal(root.get("appUser").get("id"), userId);
    }

    public static Specification<Booking> hasHotel(Long hotelId) {
        return (root, query, cb) ->
                cb.equal(root.get("hotel").get("id"), hotelId);
    }

    public static Specification<Booking> isPast() {
        return (root, query, cb) ->
                cb.lessThan(root.get("checkOutDate"), LocalDate.now());
    }

    public static Specification<Booking> isUpcoming() {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("checkInDate"), LocalDate.now());
    }


    public static Specification<Booking> hasStatus(Booking.BookingStatus status) {
        return (root, query, cb) ->
                status == null ? null :
                        cb.equal(root.get("status"), status);
    }

    public static Specification<Booking> checkInAfter(LocalDate fromDate) {
        return (root, query, cb) ->
                fromDate == null ? null :
                        cb.greaterThanOrEqualTo(root.get("checkInDate"), fromDate);
    }

    public static Specification<Booking> checkOutBefore(LocalDate toDate) {
        return (root, query, cb) ->
                toDate == null ? null :
                        cb.lessThanOrEqualTo(root.get("checkOutDate"), toDate);
    }

    public static Specification<Booking> priceBetween(Double min, Double max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null)
                return cb.between(root.get("totalPrice"), min, max);
            if (min != null)
                return cb.greaterThanOrEqualTo(root.get("totalPrice"), min);
            return cb.lessThanOrEqualTo(root.get("totalPrice"), max);
        };
    }

}
