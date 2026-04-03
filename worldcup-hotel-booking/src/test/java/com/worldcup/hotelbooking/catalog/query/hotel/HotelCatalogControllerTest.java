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

    @Test
    @DisplayName("GET /catalog/hotels -> should return catalog response with paged hotels")
    void search_ShouldReturnCatalogResponse() throws Exception {
        HotelCatalogResponseDto dto1 = new HotelCatalogResponseDto(
                1L,
                "Royal Hotel",
                "Nice hotel",
                "Nablus",
                "Palestine",
                "url1",
                BigDecimal.valueOf(300),
                BigDecimal.valueOf(4.5),
                120,
                1.2
        );

        HotelCatalogResponseDto dto2 = new HotelCatalogResponseDto(
                2L,
                "Sea View",
                "Beach hotel",
                "Gaza",
                "Palestine",
                "url2",
                BigDecimal.valueOf(450),
                BigDecimal.valueOf(4.2),
                80,
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
                "Catalog retrieved successfully"
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
                .andExpect(jsonPath("$.hotels.content.length()").value(2))
                .andExpect(jsonPath("$.hotels.content[0].id").value(1))
                .andExpect(jsonPath("$.hotels.content[0].name").value("Royal Hotel"))
                .andExpect(jsonPath("$.hotels.content[0].primaryPhotoUrl").value("url1"))
                .andExpect(jsonPath("$.hotels.content[0].startingPrice").value(300))
                .andExpect(jsonPath("$.hotels.content[0].averageRating").value(4.5))
                .andExpect(jsonPath("$.hotels.content[0].reviewCount").value(120))
                .andExpect(jsonPath("$.hotels.content[0].distanceKm").value(1.2))
                .andExpect(jsonPath("$.hotels.content[1].id").value(2))
                .andExpect(jsonPath("$.hotels.totalElements").value(2));

        verify(service, times(1)).search(any(), any());
    }

    @Test
    @DisplayName("GET /catalog/hotels -> should accept criteria params")
    void search_ShouldAcceptCriteriaParams() throws Exception {
        HotelCatalogSearchResponseDto response = new HotelCatalogSearchResponseDto(
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0),
                HotelCatalogSearchMode.NORMAL,
                false,
                "Catalog retrieved successfully"
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
                        .param("sort", "city,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchMode").value("NORMAL"))
                .andExpect(jsonPath("$.fallbackApplied").value(false))
                .andExpect(jsonPath("$.hotels.content.length()").value(0))
                .andExpect(jsonPath("$.hotels.totalElements").value(0));

        verify(service, times(1)).search(any(), any());
    }

    @Test
    @DisplayName("GET /catalog/hotels -> should return default radius mode when matchId only is provided")
    void search_WithMatchIdOnly_ShouldReturnDefaultRadiusMode() throws Exception {
        HotelCatalogResponseDto dto = new HotelCatalogResponseDto(
                1L,
                "Nearby Hotel",
                "Close to stadium",
                "Nablus",
                "Palestine",
                "url1",
                null,
                BigDecimal.valueOf(4.3),
                40,
                2.1
        );

        HotelCatalogSearchResponseDto response = new HotelCatalogSearchResponseDto(
                new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1),
                HotelCatalogSearchMode.MATCH_DEFAULT_RADIUS,
                false,
                "Showing hotels within 5.0 km of the selected stadium"
        );

        when(service.search(any(), any())).thenReturn(response);

        mockMvc.perform(get("/catalog/hotels")
                        .param("matchId", "100")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchMode").value("MATCH_DEFAULT_RADIUS"))
                .andExpect(jsonPath("$.fallbackApplied").value(false))
                .andExpect(jsonPath("$.hotels.content.length()").value(1))
                .andExpect(jsonPath("$.hotels.content[0].name").value("Nearby Hotel"));

        verify(service, times(1)).search(any(), any());
    }

    @Test
    @DisplayName("GET /catalog/hotels -> should return same city fallback mode when fallback happens")
    void search_WhenFallbackApplied_ShouldReturnFallbackResponse() throws Exception {
        HotelCatalogResponseDto dto = new HotelCatalogResponseDto(
                2L,
                "City Hotel",
                "Hotel in same city",
                "Nablus",
                "Palestine",
                "url2",
                null,
                BigDecimal.valueOf(4.0),
                22,
                null
        );

        HotelCatalogSearchResponseDto response = new HotelCatalogSearchResponseDto(
                new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1),
                HotelCatalogSearchMode.SAME_CITY_FALLBACK,
                true,
                "No hotels found within 5.0 km of the selected stadium. Showing hotels in the same city instead"
        );

        when(service.search(any(), any())).thenReturn(response);

        mockMvc.perform(get("/catalog/hotels")
                        .param("matchId", "100")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.searchMode").value("SAME_CITY_FALLBACK"))
                .andExpect(jsonPath("$.fallbackApplied").value(true))
                .andExpect(jsonPath("$.hotels.content.length()").value(1))
                .andExpect(jsonPath("$.hotels.content[0].name").value("City Hotel"));

        verify(service, times(1)).search(any(), any());
    }
}