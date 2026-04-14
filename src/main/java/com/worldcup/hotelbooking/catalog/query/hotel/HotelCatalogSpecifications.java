package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.query.hotel.exception.CheckOutBeforeCheckIn;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class HotelCatalogSpecifications {

    private static boolean empty(String value) {
        return value == null || value.isBlank();
    }

    public static Specification<Hotel> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<Hotel> nameContains(String name) {
        return (root, query, cb) ->
                empty(name)
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Hotel> cityContains(String city) {
        return (root, query, cb) ->
                empty(city)
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("city")), "%" + city.toLowerCase() + "%");
    }

    public static Specification<Hotel> countryContains(String country) {
        return (root, query, cb) ->
                empty(country)
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("country")), "%" + country.toLowerCase() + "%");
    }

    private static Specification<Hotel> booleanFilter(String field, Boolean value) {
        return (root, query, cb) ->
                value == null ? cb.conjunction() : cb.equal(root.get(field), value);
    }

    public static Specification<Hotel> hasGym(Boolean v) {
        return booleanFilter("hasGym", v);
    }

    public static Specification<Hotel> hasWifi(Boolean v) {
        return booleanFilter("hasWifi", v);
    }

    public static Specification<Hotel> hasParking(Boolean v) {
        return booleanFilter("hasParking", v);
    }

    public static Specification<Hotel> hasBreakfast(Boolean v) {
        return booleanFilter("hasBreakfast", v);
    }

    public static Specification<Hotel> hasAirConditioning(Boolean v) {
        return booleanFilter("hasAirConditioning", v);
    }

    public static Specification<Hotel> hasHeating(Boolean v) {
        return booleanFilter("hasHeating", v);
    }

    public static Specification<Hotel> hasPool(Boolean v) {
        return booleanFilter("hasPool", v);
    }

    public static Specification<Hotel> hasSpa(Boolean v) {
        return booleanFilter("hasSpa", v);
    }

    public static Specification<Hotel> hasRestaurant(Boolean v) {
        return booleanFilter("hasRestaurant", v);
    }

    public static Specification<Hotel> hasRoomService(Boolean v) {
        return booleanFilter("hasRoomService", v);
    }

    public static Specification<Hotel> hasLaundry(Boolean v) {
        return booleanFilter("hasLaundry", v);
    }

    public static Specification<Hotel> hasAirportShuttle(Boolean v) {
        return booleanFilter("hasAirportShuttle", v);
    }

    public static Specification<Hotel> hasAccessibleFacilities(Boolean v) {
        return booleanFilter("hasAccessibleFacilities", v);
    }

    public static Specification<Hotel> petFriendly(Boolean v) {
        return booleanFilter("petFriendly", v);
    }

    public static Specification<Hotel> hasElevator(Boolean v) {
        return booleanFilter("hasElevator", v);
    }

    public static Specification<Hotel> withinDistanceKm(Double lat, Double lon, Double maxKm) {
        return (root, query, cb) -> {

            if (lat == null || lon == null || maxKm == null) {
                return cb.conjunction();
            }

            double meters = maxKm * 1000.0;

            return cb.isTrue(cb.function(
                    "ST_DWithin",
                    Boolean.class,
                    root.get("location"),
                    cb.function(
                            "ST_SetSRID",
                            Object.class,
                            cb.function("ST_MakePoint", Object.class, cb.literal(lon), cb.literal(lat)),
                            cb.literal(4326)
                    ),
                    cb.literal(meters)
            ));
        };
    }

    public static Specification<Hotel> betweenDistanceKm(
            Double lat, Double lon, Double minKm, Double maxKm) {

        return (root, query, cb) -> {

            if (lat == null || lon == null || minKm == null || maxKm == null) {
                return cb.conjunction();
            }

            double minMeters = minKm * 1000.0;
            double maxMeters = maxKm * 1000.0;

            Expression<Object> point =
                    cb.function(
                            "ST_SetSRID",
                            Object.class,
                            cb.function("ST_MakePoint", Object.class, cb.literal(lon), cb.literal(lat)),
                            cb.literal(4326)
                    );

            Predicate withinMax =
                    cb.isTrue(cb.function(
                            "ST_DWithin",
                            Boolean.class,
                            root.get("location"),
                            point,
                            cb.literal(maxMeters)
                    ));

            Predicate outsideMin =
                    cb.not(cb.isTrue(cb.function(
                            "ST_DWithin",
                            Boolean.class,
                            root.get("location"),
                            point,
                            cb.literal(minMeters)
                    )));

            return cb.and(withinMax, outsideMin);
        };
    }

    public static Specification<Hotel> hasAvailability(
            LocalDate checkIn,
            LocalDate checkOut,
            Integer numberOfRooms
    ) {
        return (root, query, cb) -> {

            if (checkIn == null || checkOut == null) {
                return cb.conjunction();
            }

            if (!checkIn.isBefore(checkOut)) {
                throw new CheckOutBeforeCheckIn();
            }

            long requestedRooms = (numberOfRooms == null || numberOfRooms < 1) ? 1L : numberOfRooms.longValue();

            query.distinct(true);

            Join<Hotel, RoomType> roomJoin = root.join("roomTypes");

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<BookingRoom> bookingRoomRoot = subquery.from(BookingRoom.class);
            Join<BookingRoom, Booking> bookingJoin = bookingRoomRoot.join("booking");

            Expression<Long> bookedRoomsSum =
                    cb.coalesce(cb.sum(bookingRoomRoot.get("numberOfRooms").as(Long.class)), 0L);

            subquery.select(bookedRoomsSum);

            Predicate sameRoomType =
                    cb.equal(bookingRoomRoot.get("roomType"), roomJoin);

            Predicate overlap =
                    cb.and(
                            cb.lessThan(bookingJoin.get("checkInDate"), checkOut),
                            cb.greaterThan(bookingJoin.get("checkOutDate"), checkIn)
                    );

            Predicate statusPredicate =
                    bookingJoin.get("status").in(
                            Booking.BookingStatus.CONFIRMED,
                            Booking.BookingStatus.CHECKED_IN
                    );

            Predicate activePredicate =
                    cb.isTrue(bookingJoin.get("active"));

            subquery.where(cb.and(
                    sameRoomType,
                    overlap,
                    statusPredicate,
                    activePredicate
            ));

            Expression<Long> requiredRooms =
                    cb.sum(subquery.getSelection(), cb.literal(requestedRooms));

            return cb.greaterThanOrEqualTo(
                    roomJoin.get("totalRooms").as(Long.class),
                    requiredRooms
            );
        };
    }

    public static Specification<Hotel> minRating(java.math.BigDecimal minRating) {
        return (root, query, cb) ->
                minRating == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("averageRating"), minRating);
    }

    public static Specification<Hotel> maxRating(java.math.BigDecimal maxRating) {
        return (root, query, cb) ->
                maxRating == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(root.get("averageRating"), maxRating);
    }

    public static Specification<Hotel> minReviewCount(Integer minReviewCount) {
        return (root, query, cb) ->
                minReviewCount == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("reviewCount"), minReviewCount);
    }
}
