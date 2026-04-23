package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.availability_pricing.availability.AvailabilityServiceImpl;
import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingServiceImpl;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotelphoto.HotelPhotoRepository;
import com.worldcup.hotelbooking.catalog.hotelphoto.dto.HotelPrimaryPhotoProjection;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogSearchMode;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogSearchResponseDto;
import com.worldcup.hotelbooking.catalog.query.hotel.exception.CheckOutBeforeCheckIn;
import com.worldcup.hotelbooking.catalog.query.hotel.exception.CheckOutDateAreRequired;
import com.worldcup.hotelbooking.catalog.query.hotel.mapper.HotelCatalogMapper;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.storage.PhotoUrlResolver;
import com.worldcup.hotelbooking.tournament.match.Match;
import com.worldcup.hotelbooking.tournament.match.MatchRepository;
import com.worldcup.hotelbooking.tournament.stadium.Stadium;
import com.worldcup.hotelbooking.tournament.stadium.StadiumRepository;
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
            List.of("id", "name", "city", "distance", "price", "rating", "reviewCount");

    private static final Set<String> COMPUTED_SORT_FIELDS =
            Set.of("distance", "price");

    private static final Set<String> DB_SORT_FIELDS =
            Set.of("id", "name", "city", "rating", "reviewCount");

    private static final int MAX_HOTELS_FOR_COMPUTED_PROCESSING = 500;
    private static final int MIN_RESULTS_FOR_DEFAULT_RADIUS = 6;
    private static final List<Double> DEFAULT_RADIUS_STEPS_KM = List.of(5.0, 15.0, 30.0, 50.0);

    private final HotelPhotoRepository hotelPhotoRepository;
    private final PhotoUrlResolver photoUrlResolver;
    private final HotelRepository hotelRepository;
    private final EnhancedPricingServiceImpl enhancedPricingServiceImpl;
    private final HotelCatalogMapper hotelCatalogMapper;
    private final MatchRepository matchRepository;
    private final StadiumRepository stadiumRepository;
    private final AvailabilityServiceImpl availabilityService;

    public HotelCatalogServiceImpl(
            HotelRepository hotelRepository,
            EnhancedPricingServiceImpl enhancedPricingServiceImpl,
            HotelCatalogMapper hotelCatalogMapper,
            HotelPhotoRepository hotelPhotoRepository,
            PhotoUrlResolver photoUrlResolver,
            MatchRepository matchRepository,
            StadiumRepository stadiumRepository,
            AvailabilityServiceImpl availabilityService
    ) {
        this.hotelRepository = hotelRepository;
        this.enhancedPricingServiceImpl = enhancedPricingServiceImpl;
        this.hotelCatalogMapper = hotelCatalogMapper;
        this.hotelPhotoRepository = hotelPhotoRepository;
        this.photoUrlResolver = photoUrlResolver;
        this.matchRepository = matchRepository;
        this.stadiumRepository = stadiumRepository;
        this.availabilityService = availabilityService;
    }

    @Override
    @Transactional(readOnly = true)
    public HotelCatalogSearchResponseDto search(Pageable pageable, HotelCatalogCriteria criteria) {
        validateLocationReference(criteria);
        validateCriteria(criteria);

        if (shouldUseDefaultRadius(criteria)) {
            return searchWithDefaultRadiusAndFallback(pageable, criteria);
        }

        SearchExecutionContext context = resolveSearchExecutionContext(criteria);
        SearchInternalResult result = executeSearch(pageable, context.criteria());

        return new HotelCatalogSearchResponseDto(
                result.page(),
                HotelCatalogSearchMode.NORMAL,
                false,
                "Catalog retrieved successfully",
                result.maxPrice()
        );
    }

    private HotelCatalogSearchResponseDto searchWithDefaultRadiusAndFallback(
            Pageable pageable,
            HotelCatalogCriteria originalCriteria
    ) {
        SearchExecutionContext resolvedContext = resolveSearchExecutionContext(copyCriteria(originalCriteria));

        SearchInternalResult lastNonEmptyResult = null;
        double lastRadiusTried = DEFAULT_RADIUS_STEPS_KM.get(DEFAULT_RADIUS_STEPS_KM.size() - 1);

        for (Double radiusKm : DEFAULT_RADIUS_STEPS_KM) {
            HotelCatalogCriteria radiusCriteria = copyCriteria(resolvedContext.criteria());
            radiusCriteria.setMinDistanceKm(null);
            radiusCriteria.setMaxDistanceKm(radiusKm);

            SearchInternalResult result = executeSearch(pageable, radiusCriteria);
            lastRadiusTried = radiusKm;

            if (!result.page().isEmpty()) {
                lastNonEmptyResult = result;
            }

            if (result.page().getTotalElements() >= MIN_RESULTS_FOR_DEFAULT_RADIUS) {
                return new HotelCatalogSearchResponseDto(
                        result.page(),
                        resolveRadiusSearchMode(originalCriteria, radiusKm),
                        radiusKm > 5.0,
                        buildEnoughResultsMessage(originalCriteria, radiusKm, result.page().getTotalElements()),
                        result.maxPrice()
                );
            }
        }

        if (lastNonEmptyResult != null && !lastNonEmptyResult.page().isEmpty()) {
            return new HotelCatalogSearchResponseDto(
                    lastNonEmptyResult.page(),
                    resolveRadiusSearchMode(originalCriteria, lastRadiusTried),
                    true,
                    buildExpandedButLimitedResultsMessage(
                            originalCriteria,
                            lastRadiusTried,
                            lastNonEmptyResult.page().getTotalElements()
                    ),
                    lastNonEmptyResult.maxPrice()
            );
        }

        return new HotelCatalogSearchResponseDto(
                Page.empty(pageable),
                HotelCatalogSearchMode.NORMAL,
                true,
                buildNoResultsMessage(originalCriteria, lastRadiusTried),
                null
        );
    }

    private String buildEnoughResultsMessage(HotelCatalogCriteria criteria, double radiusKm, long total) {
        String source = criteria.getMatchId() != null ? "match stadium" : "selected stadium";

        if (radiusKm == 5.0) {
            return "Showing " + total + " hotels within 5 km of the " + source;
        }

        return "Expanded search to " + (int) radiusKm + " km around the " + source
                + " to provide more results (" + total + " hotels found)";
    }

    private String buildExpandedButLimitedResultsMessage(HotelCatalogCriteria criteria, double radiusKm, long total) {
        String source = criteria.getMatchId() != null ? "match stadium" : "selected stadium";

        return "Expanded search to " + (int) radiusKm + " km around the " + source
                + ", but only found " + total + " hotels";
    }

    private String buildNoResultsMessage(HotelCatalogCriteria criteria, double radiusKm) {
        String source = criteria.getMatchId() != null ? "match stadium" : "selected stadium";
        return "No hotels found within " + (int) radiusKm + " km of the " + source;
    }

    private HotelCatalogSearchMode resolveRadiusSearchMode(HotelCatalogCriteria criteria, double radiusKm) {
        boolean byMatch = criteria.getMatchId() != null;

        if (byMatch) {
            if (radiusKm == 5.0) return HotelCatalogSearchMode.MATCH_RADIUS_5KM;
            if (radiusKm == 15.0) return HotelCatalogSearchMode.MATCH_RADIUS_15KM;
            if (radiusKm == 30.0) return HotelCatalogSearchMode.MATCH_RADIUS_30KM;
            return HotelCatalogSearchMode.MATCH_RADIUS_50KM;
        }

        if (radiusKm == 5.0) return HotelCatalogSearchMode.STADIUM_RADIUS_5KM;
        if (radiusKm == 15.0) return HotelCatalogSearchMode.STADIUM_RADIUS_15KM;
        if (radiusKm == 30.0) return HotelCatalogSearchMode.STADIUM_RADIUS_30KM;
        return HotelCatalogSearchMode.STADIUM_RADIUS_50KM;
    }

    private boolean shouldUseDefaultRadius(HotelCatalogCriteria criteria) {
        boolean hasDefaultLocationReference = criteria.getMatchId() != null || criteria.getStadiumId() != null;
        boolean hasDistanceRange = criteria.getMinDistanceKm() != null || criteria.getMaxDistanceKm() != null;
        return hasDefaultLocationReference && !hasDistanceRange;
    }

    private SearchExecutionContext resolveSearchExecutionContext(HotelCatalogCriteria criteria) {
        HotelCatalogCriteria workingCriteria = copyCriteria(criteria);

        Stadium resolvedStadium = null;

        if (workingCriteria.getMatchId() != null) {
            Match match = matchRepository.findById(workingCriteria.getMatchId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Match not found with id: " + workingCriteria.getMatchId()
                    ));

            Stadium stadium = match.getStadium();
            if (stadium == null) {
                throw new IllegalArgumentException("The selected match does not have an assigned stadium");
            }

            if (stadium.getLatitude() == null || stadium.getLongitude() == null) {
                throw new IllegalArgumentException("The selected match stadium does not have valid coordinates");
            }

            workingCriteria.setLatitude(stadium.getLatitude());
            workingCriteria.setLongitude(stadium.getLongitude());
            resolvedStadium = stadium;

        } else if (workingCriteria.getStadiumId() != null) {
            Stadium stadium = stadiumRepository.findById(workingCriteria.getStadiumId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Stadium not found with id: " + workingCriteria.getStadiumId()
                    ));

            if (stadium.getLatitude() == null || stadium.getLongitude() == null) {
                throw new IllegalArgumentException("The selected stadium does not have valid coordinates");
            }

            workingCriteria.setLatitude(stadium.getLatitude());
            workingCriteria.setLongitude(stadium.getLongitude());
            resolvedStadium = stadium;
        }

        return new SearchExecutionContext(workingCriteria, resolvedStadium);
    }

    private SearchInternalResult executeSearch(Pageable pageable, HotelCatalogCriteria criteria) {
        validateSortFields(pageable);
        validateDistanceCriteria(criteria);
        validateSortDependencies(criteria, pageable);

        Specification<Hotel> spec = buildSpecification(criteria);

        boolean hasPriceFilter = hasPriceFilter(criteria);
        boolean hasComputedSort = hasComputedSort(pageable);
        boolean shouldComputeDistance = hasCoordinates(criteria);
        boolean shouldComputePrice = shouldComputePrice(criteria);

        if (!hasPriceFilter && !hasComputedSort && !shouldComputeDistance && !shouldComputePrice) {
            Pageable dbPageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    extractDatabaseSortableSort(pageable.getSort())
            );

            return searchOnlyWithDatabase(dbPageable, spec, criteria);
        }

        return searchWithComputedProcessing(pageable, criteria, spec);
    }

    private SearchInternalResult searchOnlyWithDatabase(
            Pageable pageable,
            Specification<Hotel> spec,
            HotelCatalogCriteria criteria
    ) {
        Page<Hotel> result = hotelRepository.findAll(spec, pageable);
        List<Hotel> hotels = result.getContent();
        Map<Long, String> primaryPhotoUrls = loadPrimaryPhotoUrls(hotels);

        int requestedRooms = criteria.getNumberOfRooms() == null ? 1 : criteria.getNumberOfRooms();

        List<HotelCatalogResponseDto> content = hotels.stream()
                .map(hotel -> hotelCatalogMapper.toDto(
                        hotel,
                        primaryPhotoUrls.get(hotel.getId()),
                        getMinimumBasePriceForRequestedRooms(hotel, requestedRooms),
                        null
                ))
                .toList();

        // Note: For database-only search, we don't calculate the global max price efficiently across all pages.
        // We'll return 2000 as a fallback or calculate it if needed. 
        // But since most searches use coordinates, they'll fall into searchWithComputedProcessing.
        BigDecimal maxPrice = content.stream()
                .map(HotelCatalogResponseDto::getMinPrice)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.valueOf(2000));

        return new SearchInternalResult(new PageImpl<>(content, pageable, result.getTotalElements()), maxPrice);
    }

    private SearchInternalResult searchWithComputedProcessing(
            Pageable pageable,
            HotelCatalogCriteria criteria,
            Specification<Hotel> spec
    ) {
        Pageable limitedPageable = PageRequest.of(
                0,
                MAX_HOTELS_FOR_COMPUTED_PROCESSING,
                extractDatabaseSortableSort(pageable.getSort())
        );

        Page<Hotel> limitedResult = hotelRepository.findAll(spec, limitedPageable);
        List<Hotel> hotels = limitedResult.getContent();

        List<HotelComputedView> allMatchingViews = hotels.stream()
                .filter(hotel -> !shouldApplyAvailabilityFiltering(criteria) || hasAnyAvailableRoom(hotel, criteria))
                .map(hotel -> toComputedView(hotel, criteria, pageable))
                .toList();

        BigDecimal maxPrice = allMatchingViews.stream()
                .map(HotelComputedView::minPrice)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.valueOf(2000));

        List<HotelComputedView> filteredViews = allMatchingViews.stream()
                .filter(view -> matchesPriceRange(view, criteria))
                .toList();

        List<HotelComputedView> sortedViews = new ArrayList<>(filteredViews);
        sortedViews.sort(buildComparator(pageable));

        List<HotelComputedView> pagedViews = slicePage(sortedViews, pageable);

        Map<Long, String> primaryPhotoUrls = loadPrimaryPhotoUrls(
                pagedViews.stream().map(HotelComputedView::hotel).toList()
        );

        List<HotelCatalogResponseDto> content = pagedViews.stream()
                .map(view -> hotelCatalogMapper.toDto(
                        view.hotel(),
                        primaryPhotoUrls.get(view.hotel().getId()),
                        view.minPrice(),
                        view.distanceKm()
                ))
                .toList();

        return new SearchInternalResult(new PageImpl<>(content, pageable, sortedViews.size()), maxPrice);
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
                .and(HotelCatalogSpecifications.minRating(criteria.getMinRating()))
                .and(HotelCatalogSpecifications.maxRating(criteria.getMaxRating()))
                .and(HotelCatalogSpecifications.minReviewCount(criteria.getMinReviewCount()))
                .and(HotelCatalogSpecifications.petFriendly(criteria.getPetFriendly()));

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

    private HotelComputedView toComputedView(Hotel hotel, HotelCatalogCriteria criteria, Pageable pageable) {
        BigDecimal minPrice;
        Double distanceKm = null;

        if (hasPriceFilter(criteria) || isSortingByPrice(pageable) || shouldComputePrice(criteria)) {
            minPrice = calculateMinimumHotelPrice(hotel, criteria);
        } else if (criteria.getNumberOfRooms() != null && criteria.getNumberOfRooms() > 1) {
            minPrice = getMinimumBasePriceForRequestedRooms(hotel, criteria.getNumberOfRooms());
        } else {
            minPrice = getMinimumBasePriceForRequestedRooms(hotel, 1);
        }

        if (hasCoordinates(criteria)) {
            distanceKm = calculateDistanceKm(
                    criteria.getLatitude(),
                    criteria.getLongitude(),
                    hotel.getLatitude(),
                    hotel.getLongitude()
            );
        }

        return new HotelComputedView(hotel, minPrice, distanceKm);
    }

    private boolean shouldApplyAvailabilityFiltering(HotelCatalogCriteria criteria) {
        return criteria.getCheckInDate() != null && criteria.getCheckOutDate() != null;
    }

    private List<RoomType> getAvailableRoomTypes(Hotel hotel, HotelCatalogCriteria criteria) {
        if (hotel.getRoomTypes() == null || hotel.getRoomTypes().isEmpty()) {
            return List.of();
        }

        if (!shouldApplyAvailabilityFiltering(criteria)) {
            return hotel.getRoomTypes();
        }

        int requestedRooms = criteria.getNumberOfRooms() == null ? 1 : criteria.getNumberOfRooms();

        return hotel.getRoomTypes().stream()
                .filter(roomType -> availabilityService.checkAvailability(
                        roomType.getId(),
                        criteria.getCheckInDate(),
                        criteria.getCheckOutDate(),
                        requestedRooms
                ))
                .toList();
    }

    private boolean hasAnyAvailableRoom(Hotel hotel, HotelCatalogCriteria criteria) {
        return !getAvailableRoomTypes(hotel, criteria).isEmpty();
    }

    private BigDecimal getMinimumBasePriceForRequestedRooms(Hotel hotel, int numberOfRooms) {
        if (hotel.getRoomTypes() == null || hotel.getRoomTypes().isEmpty()) {
            return null;
        }

        return hotel.getRoomTypes().stream()
                .map(RoomType::getBasePrice)
                .filter(Objects::nonNull)
                .map(price -> price.multiply(BigDecimal.valueOf(numberOfRooms)))
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private BigDecimal calculateMinimumHotelPrice(Hotel hotel, HotelCatalogCriteria criteria) {
        int numberOfRooms = criteria.getNumberOfRooms() == null ? 1 : criteria.getNumberOfRooms();

        List<RoomType> availableRoomTypes = getAvailableRoomTypes(hotel, criteria);

        if (availableRoomTypes.isEmpty()) {
            return null;
        }

        LocalDate checkIn = criteria.getCheckInDate();
        LocalDate checkOut = criteria.getCheckOutDate();

        if (!shouldComputePrice(criteria)) {
            return availableRoomTypes.stream()
                    .map(RoomType::getBasePrice)
                    .filter(Objects::nonNull)
                    .map(price -> price.multiply(BigDecimal.valueOf(numberOfRooms)))
                    .min(BigDecimal::compareTo)
                    .orElse(null);
        }

        BigDecimal computedMin = availableRoomTypes.stream()
                .map(roomType -> enhancedPricingServiceImpl.calculateTotalStayPrice(
                        checkIn,
                        checkOut,
                        hotel,
                        roomType,
                        numberOfRooms
                ))
                .filter(Objects::nonNull)
                .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(null);

        if (computedMin != null) {
            return computedMin;
        }

        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

        return availableRoomTypes.stream()
                .map(RoomType::getBasePrice)
                .filter(Objects::nonNull)
                .map(price -> price.multiply(BigDecimal.valueOf(numberOfRooms)).multiply(BigDecimal.valueOf(nights)))
                .min(BigDecimal::compareTo)
                .orElse(null);
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
                        p -> photoUrlResolver.resolve(p.storageKey()),
                        (existing, replacement) -> existing
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

    private void validateLocationReference(HotelCatalogCriteria criteria) {
        boolean hasMatchId = criteria.getMatchId() != null;
        boolean hasStadiumId = criteria.getStadiumId() != null;
        boolean hasLatitude = criteria.getLatitude() != null;
        boolean hasLongitude = criteria.getLongitude() != null;
        boolean hasCoordinates = hasLatitude || hasLongitude;

        if (hasLatitude != hasLongitude) {
            throw new IllegalArgumentException("latitude and longitude must be provided together");
        }

        int providedLocationReferences = 0;

        if (hasMatchId) providedLocationReferences++;
        if (hasStadiumId) providedLocationReferences++;
        if (hasCoordinates) providedLocationReferences++;

        if (providedLocationReferences > 1) {
            throw new IllegalArgumentException(
                    "Only one location reference is allowed: matchId OR stadiumId OR latitude/longitude"
            );
        }
    }

    private void validateCriteria(HotelCatalogCriteria criteria) {
        validateStayDatesIfPresent(criteria);
        validateNumberOfRooms(criteria);
        validatePriceRangeCriteria(criteria);
        validateRatingRangeCriteria(criteria);
        validateDistanceRangeCriteria(criteria);
    }

    private void validateStayDatesIfPresent(HotelCatalogCriteria criteria) {
        LocalDate checkIn = criteria.getCheckInDate();
        LocalDate checkOut = criteria.getCheckOutDate();

        if (checkIn == null && checkOut == null) {
            return;
        }

        if (checkIn == null || checkOut == null) {
            throw new CheckOutDateAreRequired("Both checkInDate and checkOutDate must be provided");
        }

        if (!checkIn.isBefore(checkOut)) {
            throw new CheckOutBeforeCheckIn();
        }
    }

    private void validateNumberOfRooms(HotelCatalogCriteria criteria) {
        if (criteria.getNumberOfRooms() != null && criteria.getNumberOfRooms() < 1) {
            throw new IllegalArgumentException("numberOfRooms must be greater than or equal to 1");
        }
    }

    private void validatePriceRangeCriteria(HotelCatalogCriteria criteria) {
        BigDecimal min = criteria.getMinTotalPrice();
        BigDecimal max = criteria.getMaxTotalPrice();

        if (min != null && min.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("minTotalPrice must be greater than or equal to 0");
        }

        if (max != null && max.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("maxTotalPrice must be greater than or equal to 0");
        }

        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("minTotalPrice cannot be greater than maxTotalPrice");
        }
    }

    private void validateRatingRangeCriteria(HotelCatalogCriteria criteria) {
        BigDecimal minRating = criteria.getMinRating();
        BigDecimal maxRating = criteria.getMaxRating();

        if (minRating != null && minRating.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("minRating must be greater than or equal to 0");
        }

        if (maxRating != null && maxRating.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("maxRating must be greater than or equal to 0");
        }

        if (maxRating != null && maxRating.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new IllegalArgumentException("maxRating must be less than or equal to 5");
        }

        if (minRating != null && minRating.compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new IllegalArgumentException("minRating must be less than or equal to 5");
        }

        if (minRating != null && maxRating != null && minRating.compareTo(maxRating) > 0) {
            throw new IllegalArgumentException("minRating cannot be greater than maxRating");
        }
    }

    private void validateDistanceRangeCriteria(HotelCatalogCriteria criteria) {
        Double min = criteria.getMinDistanceKm();
        Double max = criteria.getMaxDistanceKm();

        if (min != null && min < 0) {
            throw new IllegalArgumentException("minDistanceKm must be greater than or equal to 0");
        }

        if (max != null && max < 0) {
            throw new IllegalArgumentException("maxDistanceKm must be greater than or equal to 0");
        }

        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException("minDistanceKm cannot be greater than maxDistanceKm");
        }

        if (criteria.getMinReviewCount() != null && criteria.getMinReviewCount() < 0) {
            throw new IllegalArgumentException("minReviewCount must be greater than or equal to 0");
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

    private boolean hasCoordinates(HotelCatalogCriteria criteria) {
        return criteria.getLatitude() != null && criteria.getLongitude() != null;
    }

    private boolean shouldComputePrice(HotelCatalogCriteria criteria) {
        return criteria.getCheckInDate() != null && criteria.getCheckOutDate() != null;
    }

    private Sort extractDatabaseSortableSort(Sort requestedSort) {
        List<Sort.Order> dbOrders = requestedSort.stream()
                .filter(order -> DB_SORT_FIELDS.contains(order.getProperty()))
                .map(order -> {
                    String property = switch (order.getProperty()) {
                        case "rating" -> "averageRating";
                        case "reviewCount" -> "reviewCount";
                        default -> order.getProperty();
                    };
                    return new Sort.Order(order.getDirection(), property);
                })
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
            case "name" -> Comparator.comparing(
                    view -> view.hotel().getName(),
                    Comparator.nullsLast(String::compareToIgnoreCase)
            );
            case "city" -> Comparator.comparing(
                    view -> view.hotel().getCity(),
                    Comparator.nullsLast(String::compareToIgnoreCase)
            );
            case "distance" -> Comparator.comparing(
                    HotelComputedView::distanceKm,
                    Comparator.nullsLast(Double::compareTo)
            );
            case "price" -> Comparator.comparing(
                    HotelComputedView::minPrice,
                    Comparator.nullsLast(BigDecimal::compareTo)
            );
            case "rating" -> Comparator.comparing(
                    view -> view.hotel().getAverageRating(),
                    Comparator.nullsLast(BigDecimal::compareTo)
            );
            case "reviewCount" -> Comparator.comparing(
                    view -> view.hotel().getReviewCount(),
                    Comparator.nullsLast(Integer::compareTo)
            );
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

    private HotelCatalogCriteria copyCriteria(HotelCatalogCriteria source) {
        HotelCatalogCriteria copy = new HotelCatalogCriteria();

        copy.setName(source.getName());
        copy.setCity(source.getCity());
        copy.setCountry(source.getCountry());

        copy.setHasGym(source.getHasGym());
        copy.setHasWifi(source.getHasWifi());
        copy.setHasParking(source.getHasParking());
        copy.setHasBreakfast(source.getHasBreakfast());
        copy.setHasAirConditioning(source.getHasAirConditioning());
        copy.setHasHeating(source.getHasHeating());
        copy.setHasPool(source.getHasPool());
        copy.setHasSpa(source.getHasSpa());
        copy.setHasElevator(source.getHasElevator());
        copy.setHasRestaurant(source.getHasRestaurant());
        copy.setHasRoomService(source.getHasRoomService());
        copy.setHasLaundry(source.getHasLaundry());
        copy.setHasAirportShuttle(source.getHasAirportShuttle());
        copy.setHasAccessibleFacilities(source.getHasAccessibleFacilities());
        copy.setPetFriendly(source.getPetFriendly());

        copy.setMatchId(source.getMatchId());
        copy.setStadiumId(source.getStadiumId());

        copy.setLatitude(source.getLatitude());
        copy.setLongitude(source.getLongitude());
        copy.setMaxDistanceKm(source.getMaxDistanceKm());
        copy.setMinDistanceKm(source.getMinDistanceKm());

        copy.setCheckInDate(source.getCheckInDate());
        copy.setCheckOutDate(source.getCheckOutDate());

        copy.setMinTotalPrice(source.getMinTotalPrice());
        copy.setMaxTotalPrice(source.getMaxTotalPrice());
        copy.setNumberOfRooms(source.getNumberOfRooms());
        copy.setMinRating(source.getMinRating());
        copy.setMaxRating(source.getMaxRating());
        copy.setMinReviewCount(source.getMinReviewCount());

        return copy;
    }

    private record SearchInternalResult(
            Page<HotelCatalogResponseDto> page,
            BigDecimal maxPrice
    ) {
    }

    private record HotelComputedView(
            Hotel hotel,
            BigDecimal minPrice,
            Double distanceKm
    ) {
    }

    private record SearchExecutionContext(
            HotelCatalogCriteria criteria,
            Stadium resolvedStadium
    ) {
    }
}