package com.worldcup.hotelbooking.catalog.hotel;

import com.worldcup.hotelbooking.security.RateLimitService;
import com.worldcup.hotelbooking.catalog.hotel.dto.UpdateHotelPatchRequest;
import com.worldcup.hotelbooking.catalog.hotel.exception.HotelNotFoundException;
import com.worldcup.hotelbooking.catalog.storage.StaticResourceConfig;
import com.worldcup.hotelbooking.security.JwtTokenService;
import com.worldcup.hotelbooking.user.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = HotelController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = StaticResourceConfig.class
        ),
          excludeAutoConfiguration = {
              OAuth2ClientAutoConfiguration .class,
              OAuth2ClientWebSecurityAutoConfiguration .class,
              OAuth2ResourceServerAutoConfiguration .class
          }
)
@AutoConfigureMockMvc(addFilters = false)
class HotelCatalogControllerTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean                          // ← add this
    private CacheManager cacheManager;

    @MockitoBean
    private HotelService service;

    private Hotel buildHotel(Long id, Long ownerId) {
        AppUser owner = new AppUser();
        owner.setId(ownerId);

        Point location = GF.createPoint(new Coordinate(35.25, 32.22));

        Hotel hotel = new Hotel();
        hotel.setId(id);
        hotel.setOwner(owner);
        hotel.setName("Royal Hotel");
        hotel.setDescription("Nice hotel");
        hotel.setContactEmail("hotel@test.com");
        hotel.setContactPhone("0599999999");
        hotel.setCountry("Palestine");
        hotel.setCity("Nablus");
        hotel.setAddressLine("Main Street");
        hotel.setLocation(location);

        hotel.setLatitude(32.22);
        hotel.setLongitude(35.25);

        hotel.setStatus(HotelStatus.APPROVED);
        hotel.setHasWifi(true);
        hotel.setHasParking(true);
        hotel.setHasBreakfast(false);
        hotel.setHasAirConditioning(true);
        hotel.setHasHeating(true);
        hotel.setHasElevator(false);
        hotel.setHasRestaurant(true);
        hotel.setHasRoomService(false);
        hotel.setHasGym(true);
        hotel.setHasPool(false);
        hotel.setHasSpa(false);
        hotel.setHasLaundry(true);
        hotel.setHasAirportShuttle(false);
        hotel.setHasAccessibleFacilities(true);
        hotel.setPetFriendly(false);
        hotel.setCreatedAt(OffsetDateTime.parse("2026-03-15T10:00:00Z"));
        hotel.setUpdatedAt(OffsetDateTime.parse("2026-03-15T12:00:00Z"));

        return hotel;
    }

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("GET /hotels -> should return all approved non-deleted hotels")
    void all_ShouldReturnHotels() throws Exception {
        Hotel hotel1 = buildHotel(1L, 10L);
        Hotel hotel2 = buildHotel(2L, 11L);
        hotel2.setName("Sea View Hotel");

        given(service.findAll()).willReturn(List.of(hotel1, hotel2));

        mockMvc.perform(get("/hotels"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ownerId").value(10))
                .andExpect(jsonPath("$[0].name").value("Royal Hotel"))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[0].hasWifi").value(true))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Sea View Hotel"));

        verify(service, times(1)).findAll();
    }

    @Test
    @DisplayName("GET /hotels/{id} -> should return one hotel")
    void one_ShouldReturnHotel() throws Exception {
        Hotel hotel = buildHotel(1L, 10L);

        given(service.findById(1L)).willReturn(hotel);

        mockMvc.perform(get("/hotels/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ownerId").value(10))
                .andExpect(jsonPath("$.name").value("Royal Hotel"))
                .andExpect(jsonPath("$.country").value("Palestine"))
                .andExpect(jsonPath("$.city").value("Nablus"))
                .andExpect(jsonPath("$.latitude").value(32.22))
                .andExpect(jsonPath("$.longitude").value(35.25))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(service, times(1)).findById(1L);
    }

    @Test
    @DisplayName("POST /hotels -> should create hotel and return 201")
    void create_ShouldReturnCreatedHotel() throws Exception {
        Hotel saved = buildHotel(1L, 10L);

        given(service.create(any(Hotel.class), eq(10L))).willReturn(saved);

        String requestBody = """
                {
                  "ownerId": 10,
                  "name": "Royal Hotel",
                  "description": "Nice hotel",
                  "contactEmail": "hotel@test.com",
                  "contactPhone": "0599999999",
                  "country": "Palestine",
                  "city": "Nablus",
                  "addressLine": "Main Street",
                  "latitude": 32.22,
                  "longitude": 35.25,
                  "hasWifi": true,
                  "hasParking": true,
                  "hasBreakfast": false,
                  "hasAirConditioning": true,
                  "hasHeating": true,
                  "hasElevator": false,
                  "hasRestaurant": true,
                  "hasRoomService": false,
                  "hasGym": true,
                  "hasPool": false,
                  "hasSpa": false,
                  "hasLaundry": true,
                  "hasAirportShuttle": false,
                  "hasAccessibleFacilities": true,
                  "petFriendly": false
                }
                """;

        mockMvc.perform(post("/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/hotels/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ownerId").value(10))
                .andExpect(jsonPath("$.name").value("Royal Hotel"))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(service, times(1)).create(any(Hotel.class), eq(10L));
    }

    @Test
    @DisplayName("PUT /hotels/{id} -> should replace hotel and return updated response")
    void replace_ShouldReturnUpdatedHotel() throws Exception {
        Hotel updated = buildHotel(1L, 10L);
        updated.setName("Updated Hotel");

        given(service.replace(eq(1L), any(Hotel.class))).willReturn(updated);

        String requestBody = """
                {
                  "name": "Updated Hotel",
                  "description": "Updated description",
                  "contactEmail": "updated@test.com",
                  "contactPhone": "0566666666",
                  "country": "Palestine",
                  "city": "Nablus",
                  "addressLine": "New Address",
                  "latitude": 32.22,
                  "longitude": 35.25,
                  "hasWifi": true,
                  "hasParking": false,
                  "hasBreakfast": true,
                  "hasAirConditioning": true,
                  "hasHeating": true,
                  "hasElevator": true,
                  "hasRestaurant": true,
                  "hasRoomService": true,
                  "hasGym": false,
                  "hasPool": true,
                  "hasSpa": false,
                  "hasLaundry": true,
                  "hasAirportShuttle": false,
                  "hasAccessibleFacilities": true,
                  "petFriendly": true
                }
                """;

        mockMvc.perform(put("/hotels/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Hotel"))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(service, times(1)).replace(eq(1L), any(Hotel.class));
    }

    @Test
    @DisplayName("PATCH /hotels/{id} -> should partially update hotel")
    void patch_ShouldReturnUpdatedHotel() throws Exception {
        Hotel updated = buildHotel(1L, 10L);
        updated.setName("Patched Hotel");
        updated.setHasPool(true);

        given(service.updatePartial(eq(1L), any(UpdateHotelPatchRequest.class))).willReturn(updated);

        String requestBody = """
                {
                  "name": "Patched Hotel",
                  "hasPool": true
                }
                """;

        mockMvc.perform(patch("/hotels/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Patched Hotel"))
                .andExpect(jsonPath("$.hasPool").value(true));

        verify(service, times(1)).updatePartial(eq(1L), any(UpdateHotelPatchRequest.class));
    }

    @Test
    @DisplayName("DELETE /hotels/{id} -> should return 204")
    void delete_ShouldReturnNoContent() throws Exception {
        willDoNothing().given(service).deleteById(1L);

        mockMvc.perform(delete("/hotels/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(service, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("GET /hotels/owner/{ownerId} -> should return owner's hotels")
    void getMyHotels_ShouldReturnOwnerHotels() throws Exception {
        Hotel hotel1 = buildHotel(1L, 10L);
        Hotel hotel2 = buildHotel(2L, 10L);
        hotel2.setName("Owner Second Hotel");

        given(service.getMyHotels(10L)).willReturn(List.of(hotel1, hotel2));

        mockMvc.perform(get("/hotels/owner/{ownerId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ownerId").value(10))
                .andExpect(jsonPath("$[1].ownerId").value(10))
                .andExpect(jsonPath("$[1].name").value("Owner Second Hotel"));

        verify(service, times(1)).getMyHotels(10L);
    }

    @Test
    @DisplayName("GET /hotels/{id} -> should return 404 when hotel not found")
    void one_WhenNotFound_ShouldReturn404() throws Exception {
        given(service.findById(999L)).willThrow(new HotelNotFoundException(999L));

        mockMvc.perform(get("/hotels/{id}", 999L))
                .andExpect(status().isNotFound());
    }
}
