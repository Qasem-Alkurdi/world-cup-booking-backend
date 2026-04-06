package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.availability_pricing.availability.AvailabilityServiceImpl;
import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingServiceImpl;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotelphoto.HotelPhotoRepository;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogSearchResponseDto;
import com.worldcup.hotelbooking.catalog.query.hotel.exception.CheckOutBeforeCheckIn;
import com.worldcup.hotelbooking.catalog.query.hotel.exception.CheckOutDateAreRequired;
import com.worldcup.hotelbooking.catalog.query.hotel.mapper.HotelCatalogMapper;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.storage.PhotoUrlResolver;
import com.worldcup.hotelbooking.tournament.match.MatchRepository;
import com.worldcup.hotelbooking.tournament.stadium.StadiumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelCatalogServiceImplTest {

    @Mock
    private HotelPhotoRepository hotelPhotoRepository;

    @Mock
    private PhotoUrlResolver photoUrlResolver;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private EnhancedPricingServiceImpl enhancedPricingServiceImpl;

    @Mock
    private HotelCatalogMapper hotelCatalogMapper;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private StadiumRepository stadiumRepository;

    @Mock
    private AvailabilityServiceImpl availabilityService;

    @InjectMocks
    private HotelCatalogServiceImpl service;

    private Hotel hotel1;
    private RoomType roomType1;
    private RoomType roomType2;

    @BeforeEach
    void setUp() {
        roomType1 = new RoomType();
        roomType1.setId(101L);
        roomType1.setName("King Room");
        roomType1.setBasePrice(new BigDecimal("100.00"));
        roomType1.setTotalRooms(10);

        roomType2 = new RoomType();
        roomType2.setId(102L);
        roomType2.setName("Suite");
        roomType2.setBasePrice(new BigDecimal("180.00"));
        roomType2.setTotalRooms(5);

        hotel1 = new Hotel();
        hotel1.setId(1L);
        hotel1.setName("Test Hotel");
        hotel1.setDescription("desc");
        hotel1.setCity("Atlanta");
        hotel1.setCountry("United States");
        hotel1.setAverageRating(new BigDecimal("4.50"));
        hotel1.setReviewCount(12);
        hotel1.setLatitude(33.7490);
        hotel1.setLongitude(-84.3880);
        hotel1.setHasGym(true);
        hotel1.setHasWifi(true);
        hotel1.setHasParking(true);
        hotel1.setHasBreakfast(true);
        hotel1.setHasAirConditioning(true);
        hotel1.setHasHeating(true);
        hotel1.setHasPool(false);
        hotel1.setHasSpa(false);
        hotel1.setHasElevator(true);
        hotel1.setHasRestaurant(true);
        hotel1.setHasRoomService(true);
        hotel1.setHasLaundry(true);
        hotel1.setHasAirportShuttle(false);
        hotel1.setHasAccessibleFacilities(true);
        hotel1.setPetFriendly(false);
        hotel1.setRoomTypes(List.of(roomType1, roomType2));
    }

    @Test
    void search_shouldThrow_whenOnlyCheckInProvided() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 6, 10));

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(CheckOutDateAreRequired.class);
    }

    @Test
    void search_shouldThrow_whenCheckOutBeforeCheckIn() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 6, 10));
        criteria.setCheckOutDate(LocalDate.of(2026, 6, 9));

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(CheckOutBeforeCheckIn.class);
    }

    @Test
    void search_shouldThrow_whenNumberOfRoomsLessThanOne() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setNumberOfRooms(0);

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numberOfRooms");
    }

    @Test
    void search_shouldThrow_whenMinTotalPriceGreaterThanMaxTotalPrice() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMinTotalPrice(new BigDecimal("300"));
        criteria.setMaxTotalPrice(new BigDecimal("100"));

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minTotalPrice");
    }

    @Test
    void search_shouldThrow_whenMinDistanceGreaterThanMaxDistance() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setLatitude(33.0);
        criteria.setLongitude(-84.0);
        criteria.setMinDistanceKm(50.0);
        criteria.setMaxDistanceKm(10.0);

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minDistanceKm");
    }

    @Test
    void search_shouldThrow_whenSortFieldInvalid() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("badField").ascending());

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort field");
    }

    @Test
    void search_shouldReturnBasePrice_whenNoDatesAndNoComputedFields() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setNumberOfRooms(2);

        Pageable pageable = PageRequest.of(0, 10, Sort.by("rating").descending());

        when(hotelRepository.findAll(
                ArgumentMatchers.<Specification<Hotel>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(hotel1), pageable, 1));

        HotelCatalogResponseDto dto = new HotelCatalogResponseDto(
                hotel1.getName(),
                hotel1.getId(),
                hotel1.getDescription(),
                hotel1.getCity(),
                hotel1.getCountry(),
                null,
                hotel1.getAverageRating(),
                hotel1.getReviewCount(),
                new BigDecimal("200.00"),
                null,
                hotel1.isHasGym(),
                hotel1.isHasWifi(),
                hotel1.isHasParking(),
                hotel1.isHasBreakfast(),
                hotel1.isHasAirConditioning(),
                hotel1.isHasHeating(),
                hotel1.isHasPool(),
                hotel1.isHasSpa(),
                hotel1.isHasElevator(),
                hotel1.isHasRestaurant(),
                hotel1.isHasRoomService(),
                hotel1.isHasLaundry(),
                hotel1.isHasAirportShuttle(),
                hotel1.isHasAccessibleFacilities(),
                hotel1.isPetFriendly()
        );

        when(hotelCatalogMapper.toDto(
                eq(hotel1),
                isNull(),
                eq(new BigDecimal("200.00")),
                isNull()
        )).thenReturn(dto);

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertThat(result).isNotNull();
        assertThat(result.getHotels().getContent()).hasSize(1);
        assertThat(result.getHotels().getContent().get(0).getMinPrice())
                .isEqualByComparingTo("200.00");
    }

    @Test
    void search_shouldUseComputedPrice_whenDatesProvidedAndRoomsAvailable() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 6, 10));
        criteria.setCheckOutDate(LocalDate.of(2026, 6, 12));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("rating").descending());

        when(hotelRepository.findAll(
                ArgumentMatchers.<Specification<Hotel>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(hotel1),
                PageRequest.of(0, 500, Sort.by("averageRating").descending()),
                1
        ));

        when(availabilityService.checkAvailability(eq(101L), any(), any(), eq(1))).thenReturn(true);
        when(availabilityService.checkAvailability(eq(102L), any(), any(), eq(1))).thenReturn(true);

        when(enhancedPricingServiceImpl.calculateTotalStayPrice(
                eq(criteria.getCheckInDate()),
                eq(criteria.getCheckOutDate()),
                eq(hotel1),
                eq(roomType1),
                eq(1)
        )).thenReturn(new BigDecimal("240.00"));

        when(enhancedPricingServiceImpl.calculateTotalStayPrice(
                eq(criteria.getCheckInDate()),
                eq(criteria.getCheckOutDate()),
                eq(hotel1),
                eq(roomType2),
                eq(1)
        )).thenReturn(new BigDecimal("400.00"));

        HotelCatalogResponseDto dto = new HotelCatalogResponseDto(
                hotel1.getName(),
                hotel1.getId(),
                hotel1.getDescription(),
                hotel1.getCity(),
                hotel1.getCountry(),
                null,
                hotel1.getAverageRating(),
                hotel1.getReviewCount(),
                new BigDecimal("240.00"),
                null,
                hotel1.isHasGym(),
                hotel1.isHasWifi(),
                hotel1.isHasParking(),
                hotel1.isHasBreakfast(),
                hotel1.isHasAirConditioning(),
                hotel1.isHasHeating(),
                hotel1.isHasPool(),
                hotel1.isHasSpa(),
                hotel1.isHasElevator(),
                hotel1.isHasRestaurant(),
                hotel1.isHasRoomService(),
                hotel1.isHasLaundry(),
                hotel1.isHasAirportShuttle(),
                hotel1.isHasAccessibleFacilities(),
                hotel1.isPetFriendly()
        );

        when(hotelCatalogMapper.toDto(
                eq(hotel1),
                isNull(),
                eq(new BigDecimal("240.00")),
                isNull()
        )).thenReturn(dto);

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertThat(result.getHotels().getContent()).hasSize(1);
        assertThat(result.getHotels().getContent().get(0).getMinPrice())
                .isEqualByComparingTo("240.00");
    }

    @Test
    void search_shouldFallbackToBasePriceTimesNights_whenComputedPriceReturnsNull() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 6, 10));
        criteria.setCheckOutDate(LocalDate.of(2026, 6, 13)); // 3 nights

        Pageable pageable = PageRequest.of(0, 10, Sort.by("rating").descending());

        when(hotelRepository.findAll(
                ArgumentMatchers.<Specification<Hotel>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(hotel1),
                PageRequest.of(0, 500, Sort.by("averageRating").descending()),
                1
        ));

        when(availabilityService.checkAvailability(eq(101L), any(), any(), eq(1))).thenReturn(true);
        when(availabilityService.checkAvailability(eq(102L), any(), any(), eq(1))).thenReturn(true);

        when(enhancedPricingServiceImpl.calculateTotalStayPrice(any(), any(), eq(hotel1), eq(roomType1), eq(1)))
                .thenReturn(null);
        when(enhancedPricingServiceImpl.calculateTotalStayPrice(any(), any(), eq(hotel1), eq(roomType2), eq(1)))
                .thenReturn(null);

        HotelCatalogResponseDto dto = new HotelCatalogResponseDto(
                hotel1.getName(),
                hotel1.getId(),
                hotel1.getDescription(),
                hotel1.getCity(),
                hotel1.getCountry(),
                null,
                hotel1.getAverageRating(),
                hotel1.getReviewCount(),
                new BigDecimal("300.00"),
                null,
                hotel1.isHasGym(),
                hotel1.isHasWifi(),
                hotel1.isHasParking(),
                hotel1.isHasBreakfast(),
                hotel1.isHasAirConditioning(),
                hotel1.isHasHeating(),
                hotel1.isHasPool(),
                hotel1.isHasSpa(),
                hotel1.isHasElevator(),
                hotel1.isHasRestaurant(),
                hotel1.isHasRoomService(),
                hotel1.isHasLaundry(),
                hotel1.isHasAirportShuttle(),
                hotel1.isHasAccessibleFacilities(),
                hotel1.isPetFriendly()
        );

        when(hotelCatalogMapper.toDto(
                eq(hotel1),
                isNull(),
                eq(new BigDecimal("300.00")),
                isNull()
        )).thenReturn(dto);

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertThat(result.getHotels().getContent()).hasSize(1);
        assertThat(result.getHotels().getContent().get(0).getMinPrice())
                .isEqualByComparingTo("300.00");
    }

    @Test
    void search_shouldExcludeHotel_whenNoAvailableRooms() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 6, 10));
        criteria.setCheckOutDate(LocalDate.of(2026, 6, 12));

        Pageable pageable = PageRequest.of(0, 10, Sort.by("rating").descending());

        when(hotelRepository.findAll(
                ArgumentMatchers.<Specification<Hotel>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(hotel1),
                PageRequest.of(0, 500, Sort.by("averageRating").descending()),
                1
        ));

        when(availabilityService.checkAvailability(eq(101L), any(), any(), eq(1))).thenReturn(false);
        when(availabilityService.checkAvailability(eq(102L), any(), any(), eq(1))).thenReturn(false);

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertThat(result.getHotels().getContent()).isEmpty();
    }

    @Test
    void search_shouldRequireCoordinates_whenSortingByDistance() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("distance").ascending());

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude and longitude are required when sorting by distance");
    }

    @Test
    void search_shouldUseRequestedRoomsInAvailabilityCheck() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 6, 10));
        criteria.setCheckOutDate(LocalDate.of(2026, 6, 12));
        criteria.setNumberOfRooms(3);

        Pageable pageable = PageRequest.of(0, 10, Sort.by("rating").descending());

        when(hotelRepository.findAll(
                ArgumentMatchers.<Specification<Hotel>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(
                List.of(hotel1),
                PageRequest.of(0, 500, Sort.by("averageRating").descending()),
                1
        ));

        when(availabilityService.checkAvailability(anyLong(), any(), any(), eq(3))).thenReturn(false);

        HotelCatalogSearchResponseDto result = service.search(pageable, criteria);

        assertThat(result.getHotels().getContent()).isEmpty();

        verify(availabilityService, atLeastOnce())
                .checkAvailability(
                        anyLong(),
                        eq(criteria.getCheckInDate()),
                        eq(criteria.getCheckOutDate()),
                        eq(3)
                );
    }

    @Test
    void search_shouldThrow_whenOnlyLongitudeProvided() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setLongitude(-84.0);

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude and longitude must be provided together");
    }

    @Test
    void search_shouldThrow_whenOnlyLatitudeProvided() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setLatitude(33.0);

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitude and longitude must be provided together");
    }

    @Test
    void search_shouldThrow_whenMoreThanOneLocationReferenceProvided() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMatchId(1L);
        criteria.setLatitude(33.0);
        criteria.setLongitude(-84.0);

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only one location reference is allowed");
    }

    @Test
    void search_shouldThrow_whenNegativeMinRating() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMinRating(new BigDecimal("-1.0"));

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minRating");
    }

    @Test
    void search_shouldThrow_whenMaxRatingGreaterThanFive() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMaxRating(new BigDecimal("5.1"));

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxRating");
    }

    @Test
    void search_shouldThrow_whenMinRatingGreaterThanMaxRating() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMinRating(new BigDecimal("4.8"));
        criteria.setMaxRating(new BigDecimal("3.0"));

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minRating cannot be greater than maxRating");
    }

    @Test
    void search_shouldThrow_whenNegativeMinReviewCount() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMinReviewCount(-1);

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minReviewCount");
    }

    @Test
    void search_shouldThrow_whenNegativeMinTotalPrice() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMinTotalPrice(new BigDecimal("-10"));

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minTotalPrice");
    }

    @Test
    void search_shouldThrow_whenNegativeMaxTotalPrice() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMaxTotalPrice(new BigDecimal("-10"));

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxTotalPrice");
    }

    @Test
    void search_shouldThrow_whenNegativeMinDistanceKm() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setLatitude(33.0);
        criteria.setLongitude(-84.0);
        criteria.setMinDistanceKm(-1.0);

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minDistanceKm");
    }

    @Test
    void search_shouldThrow_whenNegativeMaxDistanceKm() {
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setLatitude(33.0);
        criteria.setLongitude(-84.0);
        criteria.setMaxDistanceKm(-1.0);

        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search(pageable, criteria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxDistanceKm");
    }
}