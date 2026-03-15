package com.worldcup.hotelbooking.catalog.query.hotel;

import com.worldcup.hotelbooking.catalog.query.hotel.dto.HotelCatalogResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
    @DisplayName("GET /catalog/hotels -> should return paged catalog results")
    void search_ShouldReturnPage() throws Exception {
        HotelCatalogResponseDto dto1 =
                new HotelCatalogResponseDto("Royal Hotel", 1L, "Nice hotel", "Nablus", "Palestine", "url1");
        HotelCatalogResponseDto dto2 =
                new HotelCatalogResponseDto("Sea View", 2L, "Beach hotel", "Gaza", "Palestine", "url2");

        when(service.search(any(), any())).thenReturn(
                new PageImpl<>(
                        List.of(dto1, dto2),
                        PageRequest.of(0, 20, Sort.by("name").ascending()),
                        2
                )
        );

        mockMvc.perform(get("/catalog/hotels")
                        .param("name", "roy")
                        .param("city", "Nablus")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "name,asc")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Royal Hotel"))
                .andExpect(jsonPath("$.content[0].primaryPhotoUrl").value("url1"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(service, times(1)).search(any(), any());
    }

    @Test
    @DisplayName("GET /catalog/hotels -> should accept criteria params")
    void search_ShouldAcceptCriteriaParams() throws Exception {
        when(service.search(any(), any())).thenReturn(
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0)
        );

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
                .andExpect(status().isOk());

        verify(service, times(1)).search(any(), any());
    }
}
