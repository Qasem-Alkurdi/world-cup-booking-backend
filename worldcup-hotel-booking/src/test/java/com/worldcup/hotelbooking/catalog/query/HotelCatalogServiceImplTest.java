package com.worldcup.hotelbooking.catalog.query;

import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingService;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotelphoto.HotelPhotoRepository;
import com.worldcup.hotelbooking.catalog.hotelphoto.dto.HotelPrimaryPhotoProjection;
import com.worldcup.hotelbooking.catalog.query.hotel.HotelCatalogCriteria;
import com.worldcup.hotelbooking.catalog.query.hotel.HotelCatalogServiceImpl;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import com.worldcup.hotelbooking.catalog.query.hotel.exception.CheckOutBeforeCheckIn;
import com.worldcup.hotelbooking.catalog.query.hotel.exception.CheckOutDateAreRequired;
import com.worldcup.hotelbooking.catalog.query.hotel.mapper.HotelCatalogMapper;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.storage.PhotoUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class HotelCatalogServiceImplTest {

    private HotelRepository hotelRepository;
    private EnhancedPricingService enhancedPricingService;
    private HotelCatalogMapper hotelCatalogMapper;
    private HotelPhotoRepository hotelPhotoRepository;
    private PhotoUrlResolver photoUrlResolver;
    private HotelCatalogServiceImpl service;

    @BeforeEach
    void setUp() {
        hotelRepository = mock(HotelRepository.class);
        enhancedPricingService = mock(EnhancedPricingService.class);
        hotelCatalogMapper = mock(HotelCatalogMapper.class);
        hotelPhotoRepository = mock(HotelPhotoRepository.class);
        photoUrlResolver = mock(PhotoUrlResolver.class);

        service = new HotelCatalogServiceImpl(
                hotelRepository,
                enhancedPricingService,
                hotelCatalogMapper,
                hotelPhotoRepository,
                photoUrlResolver
        );
    }

    private Hotel buildHotel(Long id, String name, String city, Double lat, Double lon) {
        Hotel hotel = new Hotel();
        hotel.setId(id);
        hotel.setName(name);
        hotel.setDescription("desc-" + id);
        hotel.setCity(city);
        hotel.setCountry("Palestine");
        hotel.setLatitude(lat);
        hotel.setLongitude(lon);
        return hotel;
    }

    private RoomType buildRoomType(Long id, BigDecimal basePrice) {
        RoomType roomType = new RoomType();
        roomType.setId(id);
        roomType.setName("Room-" + id);
        roomType.setBasePrice(basePrice);
        roomType.setTotalRooms(5);
        roomType.setMaxAdults(2);
        roomType.setMaxChildren(2);
        return roomType;
    }

    @Test
    @DisplayName("search -> should return mapped page when no computed filter/sort exists")
    void search_WithoutComputedFiltersOrSort_ShouldUseDatabasePaging() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name").ascending());
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();

        Hotel h1 = buildHotel(1L, "Royal", "Nablus", 32.22, 35.26);
        Hotel h2 = buildHotel(2L, "Sea View", "Gaza", 31.50, 34.46);

        HotelCatalogResponseDto dto1 =
                new HotelCatalogResponseDto("Royal", 1L, "desc-1", "Nablus", "Palestine", "url1");
        HotelCatalogResponseDto dto2 =
                new HotelCatalogResponseDto("Sea View", 2L, "desc-2", "Gaza", "Palestine", "url2");

        given(hotelRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(h1, h2), pageable, 2));

        given(hotelPhotoRepository.findPrimaryPhotosByHotelIds(List.of(1L, 2L)))
                .willReturn(List.of(
                        new HotelPrimaryPhotoProjection(1L, "hotels/1.jpg"),
                        new HotelPrimaryPhotoProjection(2L, "hotels/2.jpg")
                ));

        given(photoUrlResolver.resolve("hotels/1.jpg")).willReturn("url1");
        given(photoUrlResolver.resolve("hotels/2.jpg")).willReturn("url2");

        given(hotelCatalogMapper.toDto(h1, "url1")).willReturn(dto1);
        given(hotelCatalogMapper.toDto(h2, "url2")).willReturn(dto2);

        Page<HotelCatalogResponseDto> result = service.search(pageable, criteria);

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals("Royal", result.getContent().get(0).getName());
        assertEquals("Sea View", result.getContent().get(1).getName());

        verify(hotelRepository, times(1))
                .findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("search -> should throw when sort field is invalid")
    void search_WithInvalidSortField_ShouldThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("unknown"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(pageable, criteria)
        );

        assertTrue(ex.getMessage().contains("Invalid sort field"));
    }

    @Test
    @DisplayName("search -> should throw when distance filter exists without coordinates")
    void search_WithDistanceFilterWithoutCoordinates_ShouldThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMaxDistanceKm(10.0);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(pageable, criteria)
        );

        assertTrue(ex.getMessage().contains("latitude and longitude are required"));
    }

    @Test
    @DisplayName("search -> should throw when sorting by distance without coordinates")
    void search_SortingByDistanceWithoutCoordinates_ShouldThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("distance"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(pageable, criteria)
        );

        assertTrue(ex.getMessage().contains("sorting by distance"));
    }

    @Test
    @DisplayName("search -> should throw when price filter exists without dates")
    void search_WithPriceFilterWithoutDates_ShouldThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMinTotalPrice(BigDecimal.valueOf(100));

        assertThrows(CheckOutDateAreRequired.class,
                () -> service.search(pageable, criteria));
    }

    @Test
    @DisplayName("search -> should throw when sorting by price without dates")
    void search_SortingByPriceWithoutDates_ShouldThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();

        assertThrows(CheckOutDateAreRequired.class,
                () -> service.search(pageable, criteria));
    }

    @Test
    @DisplayName("search -> should throw when checkOut is not after checkIn")
    void search_WithInvalidDateRange_ShouldThrow() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 3, 20));
        criteria.setCheckOutDate(LocalDate.of(2026, 3, 20));

        assertThrows(CheckOutBeforeCheckIn.class,
                () -> service.search(pageable, criteria));
    }

    @Test
    @DisplayName("search -> should filter hotels by computed price range")
    void search_WithPriceFilter_ShouldFilterHotelsByComputedMinPrice() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setMinTotalPrice(BigDecimal.valueOf(200));
        criteria.setMaxTotalPrice(BigDecimal.valueOf(400));
        criteria.setCheckInDate(LocalDate.of(2026, 3, 20));
        criteria.setCheckOutDate(LocalDate.of(2026, 3, 22));
        criteria.setNumberOfRooms(1);

        Hotel h1 = buildHotel(1L, "Royal", "Nablus", 32.22, 35.26);
        Hotel h2 = buildHotel(2L, "Sea View", "Gaza", 31.50, 34.46);

        RoomType rt1 = buildRoomType(11L, BigDecimal.valueOf(150));
        RoomType rt2 = buildRoomType(12L, BigDecimal.valueOf(250));
        RoomType rt3 = buildRoomType(21L, BigDecimal.valueOf(600));

        h1.setRoomTypes(List.of(rt1, rt2));
        h2.setRoomTypes(List.of(rt3));

        Pageable limited = PageRequest.of(0, 500, Sort.by("name"));

        given(hotelRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(limited)))
                .willReturn(new PageImpl<>(List.of(h1, h2), limited, 2));

        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h1, rt1, 1))
                .willReturn(BigDecimal.valueOf(300));

        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h1, rt2, 1))
                .willReturn(BigDecimal.valueOf(350));

        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h2, rt3, 1))
                .willReturn(BigDecimal.valueOf(600));

        given(hotelPhotoRepository.findPrimaryPhotosByHotelIds(List.of(1L)))
                .willReturn(List.of(new HotelPrimaryPhotoProjection(1L, "hotels/1.jpg")));

        given(photoUrlResolver.resolve("hotels/1.jpg")).willReturn("url1");

        HotelCatalogResponseDto dto1 =
                new HotelCatalogResponseDto("Royal", 1L, "desc-1", "Nablus", "Palestine", "url1");

        given(hotelCatalogMapper.toDto(h1, "url1")).willReturn(dto1);

        Page<HotelCatalogResponseDto> result = service.search(pageable, criteria);

        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Royal", result.getContent().get(0).getName());
    }

    @Test
    @DisplayName("search -> should sort by price ascending")
    void search_SortingByPriceAscending_ShouldSortByComputedMinPrice() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").ascending());
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 3, 20));
        criteria.setCheckOutDate(LocalDate.of(2026, 3, 22));
        criteria.setNumberOfRooms(1);

        Hotel h1 = buildHotel(1L, "Hotel A", "Nablus", 32.22, 35.26);
        Hotel h2 = buildHotel(2L, "Hotel B", "Gaza", 31.50, 34.46);

        RoomType rt1 = buildRoomType(11L, BigDecimal.valueOf(500));
        RoomType rt2 = buildRoomType(22L, BigDecimal.valueOf(200));

        h1.setRoomTypes(List.of(rt1));
        h2.setRoomTypes(List.of(rt2));

        Pageable limited = PageRequest.of(0, 500, Sort.by("id").ascending());

        given(hotelRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(limited)))
                .willReturn(new PageImpl<>(List.of(h1, h2), limited, 2));

        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h1, rt1, 1))
                .willReturn(BigDecimal.valueOf(500));

        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h2, rt2, 1))
                .willReturn(BigDecimal.valueOf(200));

        given(hotelPhotoRepository.findPrimaryPhotosByHotelIds(List.of(2L, 1L)))
                .willReturn(List.of(
                        new HotelPrimaryPhotoProjection(2L, "hotels/2.jpg"),
                        new HotelPrimaryPhotoProjection(1L, "hotels/1.jpg")
                ));

        given(photoUrlResolver.resolve("hotels/2.jpg")).willReturn("url2");
        given(photoUrlResolver.resolve("hotels/1.jpg")).willReturn("url1");

        HotelCatalogResponseDto dto2 =
                new HotelCatalogResponseDto("Hotel B", 2L, "desc-2", "Gaza", "Palestine", "url2");
        HotelCatalogResponseDto dto1 =
                new HotelCatalogResponseDto("Hotel A", 1L, "desc-1", "Nablus", "Palestine", "url1");

        given(hotelCatalogMapper.toDto(h2, "url2")).willReturn(dto2);
        given(hotelCatalogMapper.toDto(h1, "url1")).willReturn(dto1);

        Page<HotelCatalogResponseDto> result = service.search(pageable, criteria);

        assertEquals(2, result.getContent().size());
        assertEquals("Hotel B", result.getContent().get(0).getName());
        assertEquals("Hotel A", result.getContent().get(1).getName());
    }

    @Test
    @DisplayName("search -> should sort by distance ascending")
    void search_SortingByDistanceAscending_ShouldSortByDistance() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("distance").ascending());
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setLatitude(32.22);
        criteria.setLongitude(35.26);

        Hotel nearHotel = buildHotel(1L, "Near Hotel", "Nablus", 32.221, 35.261);
        Hotel farHotel = buildHotel(2L, "Far Hotel", "Gaza", 31.50, 34.46);

        Pageable limited = PageRequest.of(0, 500, Sort.by("id").ascending());

        given(hotelRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(limited)))
                .willReturn(new PageImpl<>(List.of(nearHotel, farHotel), limited, 2));

        given(hotelPhotoRepository.findPrimaryPhotosByHotelIds(List.of(1L, 2L)))
                .willReturn(List.of(
                        new HotelPrimaryPhotoProjection(1L, "hotels/1.jpg"),
                        new HotelPrimaryPhotoProjection(2L, "hotels/2.jpg")
                ));

        given(photoUrlResolver.resolve("hotels/1.jpg")).willReturn("url1");
        given(photoUrlResolver.resolve("hotels/2.jpg")).willReturn("url2");

        HotelCatalogResponseDto dto1 =
                new HotelCatalogResponseDto("Near Hotel", 1L, "desc-1", "Nablus", "Palestine", "url1");
        HotelCatalogResponseDto dto2 =
                new HotelCatalogResponseDto("Far Hotel", 2L, "desc-2", "Gaza", "Palestine", "url2");

        given(hotelCatalogMapper.toDto(nearHotel, "url1")).willReturn(dto1);
        given(hotelCatalogMapper.toDto(farHotel, "url2")).willReturn(dto2);

        Page<HotelCatalogResponseDto> result = service.search(pageable, criteria);

        assertEquals(2, result.getContent().size());
        assertEquals("Near Hotel", result.getContent().get(0).getName());
        assertEquals("Far Hotel", result.getContent().get(1).getName());
    }

    @Test
    @DisplayName("search -> should slice computed result page correctly")
    void search_WithComputedSorting_ShouldSlicePage() {
        Pageable pageable = PageRequest.of(1, 1, Sort.by("price").ascending());
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();
        criteria.setCheckInDate(LocalDate.of(2026, 3, 20));
        criteria.setCheckOutDate(LocalDate.of(2026, 3, 22));
        criteria.setNumberOfRooms(1);

        Hotel h1 = buildHotel(1L, "A Hotel", "Nablus", 32.22, 35.26);
        Hotel h2 = buildHotel(2L, "B Hotel", "Gaza", 31.50, 34.46);

        RoomType rt1 = buildRoomType(11L, BigDecimal.valueOf(100));
        RoomType rt2 = buildRoomType(22L, BigDecimal.valueOf(200));

        h1.setRoomTypes(List.of(rt1));
        h2.setRoomTypes(List.of(rt2));

        Pageable limited = PageRequest.of(0, 500, Sort.by("id").ascending());

        given(hotelRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(limited)))
                .willReturn(new PageImpl<>(List.of(h1, h2), limited, 2));

        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h1, rt1, 1))
                .willReturn(BigDecimal.valueOf(100));

        given(enhancedPricingService.calculateTotalStayPrice(
                criteria.getCheckInDate(), criteria.getCheckOutDate(), h2, rt2, 1))
                .willReturn(BigDecimal.valueOf(200));

        given(hotelPhotoRepository.findPrimaryPhotosByHotelIds(List.of(2L)))
                .willReturn(List.of(new HotelPrimaryPhotoProjection(2L, "hotels/2.jpg")));

        given(photoUrlResolver.resolve("hotels/2.jpg")).willReturn("url2");

        HotelCatalogResponseDto dto2 =
                new HotelCatalogResponseDto("B Hotel", 2L, "desc-2", "Gaza", "Palestine", "url2");

        given(hotelCatalogMapper.toDto(h2, "url2")).willReturn(dto2);

        Page<HotelCatalogResponseDto> result = service.search(pageable, criteria);

        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("B Hotel", result.getContent().get(0).getName());
    }

    @Test
    @DisplayName("search -> should return empty page when repository returns no hotels")
    void search_WhenNoHotels_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
        HotelCatalogCriteria criteria = new HotelCatalogCriteria();

        given(hotelRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<HotelCatalogResponseDto> result = service.search(pageable, criteria);

        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());

        verify(hotelPhotoRepository, never()).findPrimaryPhotosByHotelIds(anyList());
    }
}
