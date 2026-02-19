package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import org.springframework.data.jpa.domain.Specification;

public class HotelCatalogSpecifications {
    public static Specification<Hotel> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<Hotel> nameContains(String name) {
        return (root, query, criteriaBuilder) ->
                name == null || name.isBlank() ? criteriaBuilder.conjunction() :
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Hotel> cityContains(String city) {
        return (root, query, criteriaBuilder) ->
                city == null || city.isBlank() ? criteriaBuilder.conjunction() :
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("city")), "%" + city.toLowerCase() + "%");
    }

    public static Specification<Hotel> countryContains(String country) {
        return (root, query, criteriaBuilder) ->
                country == null || country.isBlank() ? criteriaBuilder.conjunction() :
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("country")), "%" + country.toLowerCase() + "%");
    }

    public static Specification<Hotel> hasGym(Boolean hasGym) {
        return (root, query, criteriaBuilder) ->
                hasGym == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasGym"), hasGym);
    }

    public static Specification<Hotel> hasWifi(Boolean hasWifi) {
        return (root, query, criteriaBuilder) ->
                hasWifi == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasWifi"), hasWifi);
    }

    public static Specification<Hotel> hasParking(Boolean hasParking) {
        return (root, query, criteriaBuilder) ->
                hasParking == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasParking"), hasParking);
    }

    public static Specification<Hotel> hasBreakfast(Boolean hasBreakfast) {
        return (root, query, criteriaBuilder) ->
                hasBreakfast == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasBreakfast"), hasBreakfast);

    }

    public static Specification<Hotel> hasAirConditioning(Boolean hasAirConditioning) {
        return (root, query, criteriaBuilder) ->
                hasAirConditioning == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasAirConditioning"), hasAirConditioning);
    }

    public static Specification<Hotel> hasHeating(Boolean hasHeating) {
        return (root, query, criteriaBuilder) ->
                hasHeating == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasHeating"), hasHeating);
    }

    public static Specification<Hotel> hasElevator(Boolean hasElevator) {
        return (root, query, criteriaBuilder) ->
                hasElevator == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasElevator"), hasElevator);
    }

    public static Specification<Hotel> hasRestaurant(Boolean hasRestaurant) {
        return (root, query, criteriaBuilder) ->
                hasRestaurant == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasRestaurant"), hasRestaurant);
    }

    public static Specification<Hotel> hasPool(Boolean hasPool) {
        return (root, query, criteriaBuilder) ->
                hasPool == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasPool"), hasPool);
    }

    public static Specification<Hotel> hasRoomService(Boolean hasRoomService) {
        return (root, query, criteriaBuilder) ->
                hasRoomService == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasRoomService"), hasRoomService);
    }

    public static Specification<Hotel> hasSpa(Boolean hasSpa) {
        return (root, query, criteriaBuilder) ->
                hasSpa == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasSpa"), hasSpa);
    }

    public static Specification<Hotel> hasLaundry(Boolean hasLaundry) {
        return (root, query, criteriaBuilder) ->
                hasLaundry == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasLaundry"), hasLaundry);
    }

    public static Specification<Hotel> hasAirportShuttle(Boolean hasAirportShuttle) {
        return (root, query, criteriaBuilder) ->
                hasAirportShuttle == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasAirportShuttle"), hasAirportShuttle);
    }

    public static Specification<Hotel> hasAccessibleFacilities(Boolean hasAccessibleFacilities) {
        return (root, query, criteriaBuilder) ->
                hasAccessibleFacilities == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("hasAccessibleFacilities"), hasAccessibleFacilities);

    }

    public static Specification<Hotel> petFriendly(Boolean petFriendly) {
        return (root, query, criteriaBuilder) ->
                petFriendly == null ? criteriaBuilder.conjunction() :
                        criteriaBuilder.equal(root.get("petFriendly"), petFriendly);
    }

}
