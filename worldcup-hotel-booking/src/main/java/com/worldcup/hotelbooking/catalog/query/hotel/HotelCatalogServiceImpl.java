package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingService;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotelphoto.HotelPhotoRepository;
import com.worldcup.hotelbooking.catalog.hotelphoto.dto.HotelPrimaryPhotoProjection;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import com.worldcup.hotelbooking.catalog.query.hotel.exception.CheckOutBeforeCheckIn;
import com.worldcup.hotelbooking.catalog.query.hotel.exception.CheckOutDateAreRequired;
import com.worldcup.hotelbooking.catalog.query.hotel.mapper.HotelCatalogMapper;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.storage.PhotoUrlResolver;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HotelCatalogServiceImpl implements HotelCatalogService {

    private static final List<String> ALLOWED_SORT_FIELDS =
            List.of("id", "name", "city", "distance", "price");

    private static final Set<String> COMPUTED_SORT_FIELDS =
            Set.of("distance", "price");

    private static final Set<String> DB_SORT_FIELDS =
            Set.of("id", "name", "city");

    private static final int MAX_HOTELS_FOR_COMPUTED_PROCESSING = 500;

    private final HotelPhotoRepository hotelPhotoRepository;
    private final PhotoUrlResolver photoUrlResolver;
    private final HotelRepository hotelRepository;
    private final EnhancedPricingService enhancedPricingService;
    private final HotelCatalogMapper hotelCatalogMapper;

    public HotelCatalogServiceImpl(HotelRepository hotelRepository,
                                   EnhancedPricingService enhancedPricingService,
                                   HotelCatalogMapper hotelCatalogMapper,
                                   HotelPhotoRepository hotelPhotoRepository,
                                   PhotoUrlResolver photoUrlResolver) {
        this.hotelRepository = hotelRepository;
        this.enhancedPricingService = enhancedPricingService;
        this.hotelCatalogMapper = hotelCatalogMapper;
        this.hotelPhotoRepository = hotelPhotoRepository;
        this.photoUrlResolver = photoUrlResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HotelCatalogResponseDto> search(Pageable pageable, HotelCatalogCriteria criteria) {
        validateSortFields(pageable);
        validateDistanceCriteria(criteria);
        validateSortDependencies(criteria, pageable);

        Specification<Hotel> spec = buildSpecification(criteria);

        boolean hasPriceFilter = hasPriceFilter(criteria);
        boolean hasComputedSort = hasComputedSort(pageable);

        if (!hasPriceFilter && !hasComputedSort) {
            return searchOnlyWithDatabase(pageable, spec);
        }

        return searchWithComputedProcessing(pageable, criteria, spec);
    }

    private Page<HotelCatalogResponseDto> searchOnlyWithDatabase(Pageable pageable, Specification<Hotel> spec) {
        Page<Hotel> result = hotelRepository.findAll(spec, pageable);
        List<Hotel> hotels = result.getContent();
        Map<Long, String> primaryPhotoUrls = loadPrimaryPhotoUrls(hotels);

        List<HotelCatalogResponseDto> content = hotels.stream()
                .map(hotel -> hotelCatalogMapper.toDto(
                        hotel,
                        primaryPhotoUrls.get(hotel.getId())
                ))
                .toList();

        return new PageImpl<>(content, pageable, result.getTotalElements());
    }

    private Page<HotelCatalogResponseDto> searchWithComputedProcessing(
            Pageable pageable,
            HotelCatalogCriteria criteria,
            Specification<Hotel> spec
    ) {
        if (hasPriceFilter(criteria) || isSortingByPrice(pageable)) {
            validateDateRange(criteria.getCheckInDate(), criteria.getCheckOutDate());
        }

        Pageable limitedPageable = PageRequest.of(
                0,
                MAX_HOTELS_FOR_COMPUTED_PROCESSING,
                extractDatabaseSortableSort(pageable.getSort())
        );

        List<Hotel> hotels = hotelRepository.findAll(spec, limitedPageable).getContent();

        List<HotelComputedView> computedViews = hotels.stream()
                .map(hotel -> toComputedView(hotel, criteria))
                .filter(view -> matchesPriceRange(view, criteria))
                .toList();

        List<HotelComputedView> sortedViews = new ArrayList<>(computedViews);
        sortedViews.sort(buildComparator(pageable));

        List<HotelComputedView> pagedViews = slicePage(sortedViews, pageable);
        List<Hotel> pagedHotels = pagedViews.stream()
                .map(HotelComputedView::hotel)
                .toList();

        Map<Long, String> primaryPhotoUrls = loadPrimaryPhotoUrls(pagedHotels);

        List<HotelCatalogResponseDto> content = pagedHotels.stream()
                .map(hotel -> hotelCatalogMapper.toDto(
                        hotel,
                        primaryPhotoUrls.get(hotel.getId())
                ))
                .toList();

        return new PageImpl<>(content, pageable, sortedViews.size());
    }

    private Specification<Hotel> buildSpecification(HotelCatalogCriteria criteria) {
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
                .and(HotelCatalogSpecifications.hasElevator(criteria.getHasElevator()))
                .and(HotelCatalogSpecifications.hasRestaurant(criteria.getHasRestaurant()))
                .and(HotelCatalogSpecifications.hasRoomService(criteria.getHasRoomService()))
                .and(HotelCatalogSpecifications.hasLaundry(criteria.getHasLaundry()))
                .and(HotelCatalogSpecifications.hasAirportShuttle(criteria.getHasAirportShuttle()))
                .and(HotelCatalogSpecifications.hasAccessibleFacilities(criteria.getHasAccessibleFacilities()))
                .and(HotelCatalogSpecifications.petFriendly(criteria.getPetFriendly()))
                .and(HotelCatalogSpecifications.hasAvailability(
                        criteria.getCheckInDate(),
                        criteria.getCheckOutDate()
                ));

        if (criteria.getMinDistanceKm() != null && criteria.getMaxDistanceKm() != null) {
            spec = spec.and(HotelCatalogSpecifications.betweenDistanceKm(
                    criteria.getLatitude(),
                    criteria.getLongitude(),
                    criteria.getMinDistanceKm(),
                    criteria.getMaxDistanceKm()
            ));
        } else if (criteria.getMaxDistanceKm() != null) {
            spec = spec.and(HotelCatalogSpecifications.withinDistanceKm(
                    criteria.getLatitude(),
                    criteria.getLongitude(),
                    criteria.getMaxDistanceKm()
            ));
        }

        return spec;
    }

    private HotelComputedView toComputedView(Hotel hotel, HotelCatalogCriteria criteria) {
        BigDecimal minPrice = null;
        Double distanceKm = null;

        if (hasPriceFilter(criteria) || criteriaRequiresPriceSort(criteria)) {
            minPrice = calculateMinimumHotelPrice(hotel, criteria);
        }

        if (criteriaRequiresDistanceSort(criteria)) {
            distanceKm = calculateDistanceKm(
                    criteria.getLatitude(),
                    criteria.getLongitude(),
                    hotel.getLatitude(),
                    hotel.getLongitude()
            );
        }

        return new HotelComputedView(hotel, minPrice, distanceKm);
    }

    private BigDecimal calculateMinimumHotelPrice(Hotel hotel, HotelCatalogCriteria criteria) {
        LocalDate checkIn = criteria.getCheckInDate();
        LocalDate checkOut = criteria.getCheckOutDate();
        int numberOfRooms = criteria.getNumberOfRooms() == null ? 1 : criteria.getNumberOfRooms();

        BigDecimal minPrice = null;

        for (RoomType roomType : hotel.getRoomTypes()) {
            BigDecimal total = enhancedPricingService.calculateTotalStayPrice(
                    checkIn,
                    checkOut,
                    hotel,
                    roomType,
                    numberOfRooms
            );

            if (minPrice == null || total.compareTo(minPrice) < 0) {
                minPrice = total;
            }
        }

        return minPrice;
    }

    private boolean matchesPriceRange(HotelComputedView view, HotelCatalogCriteria criteria) {
        if (!hasPriceFilter(criteria)) {
            return true;
        }

        if (view.minPrice() == null) {
            return false;
        }

        BigDecimal min = criteria.getMinTotalPrice();
        BigDecimal max = criteria.getMaxTotalPrice();

        if (min != null && view.minPrice().compareTo(min) < 0) {
            return false;
        }

        return max == null || view.minPrice().compareTo(max) <= 0;
    }

    private Map<Long, String> loadPrimaryPhotoUrls(List<Hotel> hotels) {
        List<Long> hotelIds = hotels.stream()
                .map(Hotel::getId)
                .toList();

        if (hotelIds.isEmpty()) {
            return Map.of();
        }

        return hotelPhotoRepository.findPrimaryPhotosByHotelIds(hotelIds).stream()
                .collect(Collectors.toMap(
                        HotelPrimaryPhotoProjection::hotelId,
                        p -> photoUrlResolver.resolve(p.storageKey())
                ));
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

    private void validateSortDependencies(HotelCatalogCriteria criteria, Pageable pageable) {
        if (isSortingByDistance(pageable)) {
            if (criteria.getLatitude() == null || criteria.getLongitude() == null) {
                throw new IllegalArgumentException(
                        "latitude and longitude are required when sorting by distance"
                );
            }
        }

        if (isSortingByPrice(pageable)) {
            validateDateRange(criteria.getCheckInDate(), criteria.getCheckOutDate());
        }
    }

    private boolean hasPriceFilter(HotelCatalogCriteria criteria) {
        return criteria.getMinTotalPrice() != null || criteria.getMaxTotalPrice() != null;
    }

    private boolean hasComputedSort(Pageable pageable) {
        return pageable.getSort().stream()
                .map(Sort.Order::getProperty)
                .anyMatch(COMPUTED_SORT_FIELDS::contains);
    }

    private boolean isSortingByDistance(Pageable pageable) {
        return pageable.getSort().stream()
                .anyMatch(order -> order.getProperty().equals("distance"));
    }

    private boolean isSortingByPrice(Pageable pageable) {
        return pageable.getSort().stream()
                .anyMatch(order -> order.getProperty().equals("price"));
    }

    private boolean criteriaRequiresDistanceSort(HotelCatalogCriteria criteria) {
        return criteria.getLatitude() != null && criteria.getLongitude() != null;
    }

    private boolean criteriaRequiresPriceSort(HotelCatalogCriteria criteria) {
        return criteria.getCheckInDate() != null && criteria.getCheckOutDate() != null;
    }

    private Sort extractDatabaseSortableSort(Sort requestedSort) {
        List<Sort.Order> dbOrders = requestedSort.stream()
                .filter(order -> DB_SORT_FIELDS.contains(order.getProperty()))
                .toList();

        if (dbOrders.isEmpty()) {
            return Sort.by("id").ascending();
        }

        return Sort.by(dbOrders);
    }

    private Comparator<HotelComputedView> buildComparator(Pageable pageable) {
        Comparator<HotelComputedView> comparator = null;

        for (Sort.Order order : pageable.getSort()) {
            Comparator<HotelComputedView> fieldComparator = comparatorForField(order.getProperty());

            if (order.isDescending()) {
                fieldComparator = fieldComparator.reversed();
            }

            comparator = comparator == null ? fieldComparator : comparator.thenComparing(fieldComparator);
        }

        if (comparator == null) {
            comparator = Comparator.comparing(view -> view.hotel().getId());
        }

        return comparator.thenComparing(view -> view.hotel().getId());
    }

    private Comparator<HotelComputedView> comparatorForField(String field) {
        return switch (field) {
            case "id" -> Comparator.comparing(view -> view.hotel().getId(), Comparator.nullsLast(Long::compareTo));
            case "name" ->
                    Comparator.comparing(view -> view.hotel().getName(), Comparator.nullsLast(String::compareToIgnoreCase));
            case "city" ->
                    Comparator.comparing(view -> view.hotel().getCity(), Comparator.nullsLast(String::compareToIgnoreCase));
            case "distance" ->
                    Comparator.comparing(HotelComputedView::distanceKm, Comparator.nullsLast(Double::compareTo));
            case "price" ->
                    Comparator.comparing(HotelComputedView::minPrice, Comparator.nullsLast(BigDecimal::compareTo));
            default -> throw new IllegalArgumentException("Unsupported sort field: " + field);
        };
    }

    private List<HotelComputedView> slicePage(List<HotelComputedView> hotels, Pageable pageable) {
        int start = (int) pageable.getOffset();

        if (start >= hotels.size()) {
            return List.of();
        }

        int end = Math.min(start + pageable.getPageSize(), hotels.size());
        return hotels.subList(start, end);
    }

    private void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new CheckOutDateAreRequired("checkInDate and checkOutDate are required for price filtering");
        }

        if (!checkIn.isBefore(checkOut)) {
            throw new CheckOutBeforeCheckIn();
        }
    }

    /**
     * Haversine formula
     */
    private Double calculateDistanceKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }

        final double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private record HotelComputedView(
            Hotel hotel,
            BigDecimal minPrice,
            Double distanceKm
    ) {
    }
}
