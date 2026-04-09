package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogSearchMode;
import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogSearchResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HotelCatalogControllerTest {

    private MockMvc mockMvc;
    private HotelCatalogService service;

    @BeforeEach
    void setUp() {
        service = mock(HotelCatalogService.class);
        HotelCatalogController controller = new HotelCatalogController(service);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    private HotelCatalogResponseDto hotelDto(
            Long id,
            String name,
            String description,
            String city,
            String country,
            String primaryPhotoUrl,
            BigDecimal averageRating,
            Integer reviewCount,
            BigDecimal minPrice,
            Double distanceKm
    ) {
        return new HotelCatalogResponseDto(
                name,
                id,
                description,
                city,
                country,
                primaryPhotoUrl,
                averageRating,
                reviewCount,
                minPrice,
                distanceKm,
                true,   // hasGym
                true,   // hasWifi
                true,   // hasParking
                true,   // hasBreakfast
                true,   // hasAirConditioning
                true,   // hasHeating
                false,  // hasPool
                false,  // hasSpa
                true,   // hasElevator
                true,   // hasRestaurant
                true,   // hasRoomService
                true,   // hasLaundry
                false,  // hasAirportShuttle
                true,   // hasAccessibleFacilities
                false   // petFriendly
        );
    }

    @Test
    @DisplayName("GET /catalog/hotels -> should return catalog response with paged hotels")
    void search_ShouldReturnCatalogResponse() throws Exception {
        HotelCatalogResponseDto dto1 = hotelDto(
                1L,
                "Royal Hotel",
                "Nice hotel",
                "Nablus",
                "Palestine",
                "url1",
                BigDecimal.valueOf(4.5),
                120,
                BigDecimal.valueOf(300),
                1.2
        );

        HotelCatalogResponseDto dto2 = hotelDto(
                2L,
                "Sea View",
                "Beach hotel",
                "Gaza",
                "Palestine",
                "url2",
                BigDecimal.valueOf(4.2),
                80,
                BigDecimal.valueOf(450),
                3.8
        );

        HotelCatalogSearchResponseDto response = new HotelCatalogSearchResponseDto(
                new PageImpl<>(
                        List.of(dto1, dto2),
                        PageRequest.of(0, 20, Sort.by("name").ascending()),
                        2
                ),
                HotelCatalogSearchMode.NORMAL,
                false,
                "Catalog retrieved successfully",
                BigDecimal.valueOf(450)
        );

        when(service.search(any(), any())).thenReturn(response);

        mockMvc.perform(get("/catalog/hotels")
                        .param("name", "roy")
                        .param("city", "Nablus")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "name,asc")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchMode").value("NORMAL"))
                .andExpect(jsonPath("$.fallbackApplied").value(false))
                .andExpect(jsonPath("$.message").value("Catalog retrieved successfully"))
                .andExpect(jsonPath("$.maxTotalPriceFound").value(450))
                .andExpect(jsonPath("$.hotels.content.length()").value(2))
                .andExpect(jsonPath("$.hotels.content[0].id").value(1))
                .andExpect(jsonPath("$.hotels.content[0].name").value("Royal Hotel"))
                .andExpect(jsonPath("$.hotels.content[0].primaryPhotoUrl").value("url1"))
                .andExpect(jsonPath("$.hotels.content[0].minPrice").value(300))
                .andExpect(jsonPath("$.hotels.content[0].averageRating").value(4.5))
                .andExpect(jsonPath("$.hotels.content[0].reviewCount").value(120))
                .andExpect(jsonPath("$.hotels.content[0].distanceKm").value(1.2))
                .andExpect(jsonPath("$.hotels.content[1].id").value(2))
                .andExpect(jsonPath("$.hotels.totalElements").value(2))
                .andExpect(jsonPath("$.hotels.size").value(20))
                .andExpect(jsonPath("$.hotels.number").value(0));

        verify(service, times(1)).search(any(), any());
    }

    @Test
    @DisplayName("GET /catalog/hotels -> should accept criteria params")
    void search_ShouldAcceptCriteriaParams() throws Exception {
        HotelCatalogSearchResponseDto response = new HotelCatalogSearchResponseDto(
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0),
                HotelCatalogSearchMode.NORMAL,
                false,
                "Catalog retrieved successfully",
                null
        );

        when(service.search(any(), any())).thenReturn(response);

        mockMvc.perform(get("/catalog/hotels")
                        .param("country", "Palestine")
                        .param("hasWifi", "true")
                        .param("hasGym", "true")
                        .param("latitude", "32.22")
                        .param("longitude", "35.26")
                        .param("maxDistanceKm", "10")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "city,desc")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchMode").value("NORMAL"))
                .andExpect(jsonPath("$.fallbackApplied").value(false))
                .andExpect(jsonPath("$.message").value("Catalog retrieved successfully"))
                .andExpect(jsonPath("$.maxTotalPriceFound").isEmpty())
                .andExpect(jsonPath("$.hotels.content.length()").value(0))
                .andExpect(jsonPath("$.hotels.totalElements").value(0))
                .andExpect(jsonPath("$.hotels.size").value(10))
                .andExpect(jsonPath("$.hotels.number").value(0));

        verify(service, times(1)).search(any(), any());
    }

    @Test
    @DisplayName("GET /catalog/hotels -> should return match radius mode when matchId only is provided")
    void search_WithMatchIdOnly_ShouldReturn5KmMode() throws Exception {
        HotelCatalogResponseDto dto = hotelDto(
                1L,
                "Nearby Hotel",
                "Close to stadium",
                "Nablus",
                "Palestine",
                "url1",
                BigDecimal.valueOf(4.3),
                40,
                null,
                2.1
        );

        HotelCatalogSearchResponseDto response = new HotelCatalogSearchResponseDto(
                new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1),
                HotelCatalogSearchMode.MATCH_RADIUS_5KM,
                false,
                "Showing hotels within 5 km of the match stadium",
                null
        );

        when(service.search(any(), any())).thenReturn(response);

        mockMvc.perform(get("/catalog/hotels")
                        .param("matchId", "100")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchMode").value("MATCH_RADIUS_5KM"))
                .andExpect(jsonPath("$.fallbackApplied").value(false))
                .andExpect(jsonPath("$.message").value("Showing hotels within 5 km of the match stadium"))
                .andExpect(jsonPath("$.maxTotalPriceFound").isEmpty())
                .andExpect(jsonPath("$.hotels.content.length()").value(1))
                .andExpect(jsonPath("$.hotels.content[0].name").value("Nearby Hotel"))
                .andExpect(jsonPath("$.hotels.totalElements").value(1));

        verify(service, times(1)).search(any(), any());
    }

    @Test
    @DisplayName("GET /catalog/hotels -> should return 15 km radius mode when search expands from 5 km")
    void search_WhenRadiusExpandedTo15Km_ShouldReturn15KmResponse() throws Exception {
        HotelCatalogResponseDto dto = hotelDto(
                2L,
                "City Hotel",
                "Hotel found after expanding radius",
                "Ciudad de Mexico",
                "Mexico",
                "url2",
                BigDecimal.valueOf(4.0),
                22,
                null,
                6.7
        );

        HotelCatalogSearchResponseDto response = new HotelCatalogSearchResponseDto(
                new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1),
                HotelCatalogSearchMode.MATCH_RADIUS_15KM,
                true,
                "No hotels found in the smaller radius. Expanded search to 15 km around the match stadium",
                null
        );

        when(service.search(any(), any())).thenReturn(response);

        mockMvc.perform(get("/catalog/hotels")
                        .param("matchId", "100")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchMode").value("MATCH_RADIUS_15KM"))
                .andExpect(jsonPath("$.fallbackApplied").value(true))
                .andExpect(jsonPath("$.message").value("No hotels found in the smaller radius. Expanded search to 15 km around the match stadium"))
                .andExpect(jsonPath("$.maxTotalPriceFound").isEmpty())
                .andExpect(jsonPath("$.hotels.content.length()").value(1))
                .andExpect(jsonPath("$.hotels.content[0].name").value("City Hotel"))
                .andExpect(jsonPath("$.hotels.totalElements").value(1));

        verify(service, times(1)).search(any(), any());
    }

    @Test
    @DisplayName("GET /catalog/hotels -> should return 30 km radius mode when search expands again")
    void search_WhenRadiusExpandedTo30Km_ShouldReturn30KmResponse() throws Exception {
        HotelCatalogResponseDto dto = hotelDto(
                3L,
                "Farther Hotel",
                "Hotel found after expanding to 30 km",
                "Apodaca",
                "Mexico",
                "url3",
                BigDecimal.valueOf(4.1),
                18,
                null,
                22.4
        );

        HotelCatalogSearchResponseDto response = new HotelCatalogSearchResponseDto(
                new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1),
                HotelCatalogSearchMode.MATCH_RADIUS_30KM,
                true,
                "No hotels found in the smaller radius. Expanded search to 30 km around the match stadium",
                null
        );

        when(service.search(any(), any())).thenReturn(response);

        mockMvc.perform(get("/catalog/hotels")
                        .param("matchId", "100")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchMode").value("MATCH_RADIUS_30KM"))
                .andExpect(jsonPath("$.fallbackApplied").value(true))
                .andExpect(jsonPath("$.message").value("No hotels found in the smaller radius. Expanded search to 30 km around the match stadium"))
                .andExpect(jsonPath("$.maxTotalPriceFound").isEmpty())
                .andExpect(jsonPath("$.hotels.content.length()").value(1))
                .andExpect(jsonPath("$.hotels.content[0].name").value("Farther Hotel"))
                .andExpect(jsonPath("$.hotels.totalElements").value(1));

        verify(service, times(1)).search(any(), any());
    }

    @Test
    @DisplayName("GET /catalog/hotels -> should return stadium 5 km mode when stadiumId only is provided")
    void search_WithStadiumIdOnly_ShouldReturnStadium5KmMode() throws Exception {
        HotelCatalogResponseDto dto = hotelDto(
                4L,
                "Stadium Nearby Hotel",
                "Close to selected stadium",
                "Seattle",
                "United States",
                "url4",
                BigDecimal.valueOf(4.6),
                55,
                null,
                1.1
        );

        HotelCatalogSearchResponseDto response = new HotelCatalogSearchResponseDto(
                new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1),
                HotelCatalogSearchMode.STADIUM_RADIUS_5KM,
                false,
                "Showing hotels within 5 km of the selected stadium",
                null
        );

        when(service.search(any(), any())).thenReturn(response);

        mockMvc.perform(get("/catalog/hotels")
                        .param("stadiumId", "200")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchMode").value("STADIUM_RADIUS_5KM"))
                .andExpect(jsonPath("$.fallbackApplied").value(false))
                .andExpect(jsonPath("$.message").value("Showing hotels within 5 km of the selected stadium"))
                .andExpect(jsonPath("$.maxTotalPriceFound").isEmpty())
                .andExpect(jsonPath("$.hotels.content.length()").value(1))
                .andExpect(jsonPath("$.hotels.content[0].name").value("Stadium Nearby Hotel"))
                .andExpect(jsonPath("$.hotels.totalElements").value(1));

        verify(service, times(1)).search(any(), any());
    }
}