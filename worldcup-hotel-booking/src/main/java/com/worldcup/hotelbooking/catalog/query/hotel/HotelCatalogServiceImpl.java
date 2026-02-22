package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingService;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import com.worldcup.hotelbooking.catalog.query.hotel.exeption.CheckOutBeforeCheckIn;
import com.worldcup.hotelbooking.catalog.query.hotel.exeption.CheckOutDateAreRequired;
import com.worldcup.hotelbooking.catalog.query.hotel.mapper.HotelCatalogMapper;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class HotelCatalogServiceImpl implements HotelCatalogService {

    private static final List<String> ALLOWED_SORT_FIELDS =
            List.of("id", "name", "city");


    private static final int MAX_HOTELS_FOR_PRICE_FILTER = 500;

    private final HotelRepository hotelRepository;
    private final EnhancedPricingService enhancedPricingService;

    HotelCatalogServiceImpl(HotelRepository hotelRepository,
                            EnhancedPricingService enhancedPricingService) {
        this.hotelRepository = hotelRepository;
        this.enhancedPricingService = enhancedPricingService;
    }

    @Override
    public Page<HotelCatalogResponseDto> search(Pageable pageable, HotelCatalogCriteria criteria) {
        validateSortFields(pageable);
        validateDistanceCriteria(criteria);

        Specification<Hotel> distanceSpec = null;

        if (criteria.getMinDistanceKm() != null && criteria.getMaxDistanceKm() != null) {
            distanceSpec = HotelCatalogSpecifications.betweenDistanceKm(
                    criteria.getLatitude(),
                    criteria.getLongitude(),
                    criteria.getMinDistanceKm(),
                    criteria.getMaxDistanceKm()
            );
        } else if (criteria.getMaxDistanceKm() != null) {
            distanceSpec = HotelCatalogSpecifications.withinDistanceKm(
                    criteria.getLatitude(),
                    criteria.getLongitude(),
                    criteria.getMaxDistanceKm()
            );
        }

        Specification<Hotel> spec = Specification
                .where(HotelCatalogSpecifications.notDeleted())
                .and(HotelCatalogSpecifications.nameContains(criteria.getName()))
                .and(HotelCatalogSpecifications.cityContains(criteria.getCity()))
                .and(HotelCatalogSpecifications.countryContains(criteria.getCountry()))
                .and(HotelCatalogSpecifications.hasGym(criteria.getHasGym()))
                .and(HotelCatalogSpecifications.hasWifi(criteria.getHasWifi()))
                .and(HotelCatalogSpecifications.hasParking(criteria.getHasParking()))
                .and(HotelCatalogSpecifications.hasBreakfast(criteria.getHasBreakfast()))
                .and(HotelCatalogSpecifications.hasAirConditioning(criteria.getHasAirConditioning()))
                .and(HotelCatalogSpecifications.hasHeating(criteria.getHasHeating()))
                .and(HotelCatalogSpecifications.hasPool(criteria.getHasPool()))
                .and(HotelCatalogSpecifications.hasSpa(criteria.getHasSpa()))
                .and(HotelCatalogSpecifications.hasRestaurant(criteria.getHasRestaurant()))
                .and(HotelCatalogSpecifications.hasRoomService(criteria.getHasRoomService()))
                .and(HotelCatalogSpecifications.hasLaundry(criteria.getHasLaundry()))
                .and(HotelCatalogSpecifications.hasAirportShuttle(criteria.getHasAirportShuttle()))
                .and(HotelCatalogSpecifications.hasAccessibleFacilities(criteria.getHasAccessibleFacilities()))
                .and(distanceSpec)
                .and(HotelCatalogSpecifications.hasAvailability(criteria.getCheckInDate(), criteria.getCheckOutDate()))
                .and(HotelCatalogSpecifications.petFriendly(criteria.getPetFriendly())
                        .and(HotelCatalogSpecifications.hasElevator(criteria.getHasElevator())));

        if (hasPriceFilter(criteria)) {
            validateDateRange(criteria.getCheckInDate(), criteria.getCheckOutDate());


            Pageable limitedPageable = PageRequest.of(0, MAX_HOTELS_FOR_PRICE_FILTER, pageable.getSort());
            List<Hotel> hotels = hotelRepository.findAll(spec, limitedPageable).getContent();

            List<Hotel> filtered = new ArrayList<>();
            for (Hotel hotel : hotels) {
                if (hotelMatchesPriceRange(hotel, criteria)) {
                    filtered.add(hotel);
                }
            }

            List<Hotel> paged = slicePage(filtered, pageable);
            List<HotelCatalogResponseDto> content = paged.stream()
                    .map(HotelCatalogMapper::toDto)
                    .toList();

            return new PageImpl<>(content, pageable, filtered.size());
        }

        Page<Hotel> result = hotelRepository.findAll(spec, pageable);

        List<HotelCatalogResponseDto> content = result.getContent().stream()
                .map(HotelCatalogMapper::toDto)
                .toList();

        return new PageImpl<>(content, pageable, result.getTotalElements());
    }


    private boolean hotelMatchesPriceRange(Hotel hotel, HotelCatalogCriteria criteria) {
        LocalDate checkIn = criteria.getCheckInDate();
        LocalDate checkOut = criteria.getCheckOutDate();
        int numberOfRooms = criteria.getNumberOfRooms() == null ? 1 : criteria.getNumberOfRooms();
        BigDecimal min = criteria.getMinTotalPrice();
        BigDecimal max = criteria.getMaxTotalPrice();

        for (RoomType roomType : hotel.getRoomsType()) {
            BigDecimal total = enhancedPricingService.calculateTotalStayPrice(
                    checkIn, checkOut, hotel, roomType, numberOfRooms);
            if (priceWithinRange(total, min, max)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPriceFilter(HotelCatalogCriteria criteria) {
        return criteria.getMinTotalPrice() != null || criteria.getMaxTotalPrice() != null;
    }


    private void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new CheckOutDateAreRequired("checkInDate and checkOutDate are required for price filtering");
        }
        if (!checkIn.isBefore(checkOut)) {
            throw new CheckOutBeforeCheckIn();
        }
    }


    private void validateDistanceCriteria(HotelCatalogCriteria criteria) {
        boolean hasDistanceFilter = criteria.getMinDistanceKm() != null
                || criteria.getMaxDistanceKm() != null;

        if (hasDistanceFilter) {
            if (criteria.getLatitude() == null || criteria.getLongitude() == null) {
                throw new IllegalArgumentException(
                        "latitude and longitude are required when filtering by distance"
                );
            }
        }
    }

    private boolean priceWithinRange(BigDecimal total, BigDecimal min, BigDecimal max) {
        if (min != null && total.compareTo(min) < 0) {
            return false;
        }
        return max == null || total.compareTo(max) <= 0;
    }

    private List<Hotel> slicePage(List<Hotel> hotels, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= hotels.size()) {
            return List.of();
        }
        int end = Math.min(start + pageable.getPageSize(), hotels.size());
        return hotels.subList(start, end);
    }

    private void validateSortFields(Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Invalid sort field: " + order.getProperty()
                                + ". Allowed fields: " + ALLOWED_SORT_FIELDS
                );
            }
        }
    }
}