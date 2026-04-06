package com.worldcup.hotelbooking.catalog.roomtype;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RoomTypeControllerTest {

    private MockMvc mockMvc;
    private RoomTypeService service;

    @BeforeEach
    void setUp() {
        service = mock(RoomTypeService.class);
        RoomTypeController controller = new RoomTypeController(service);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private RoomType buildRoomType(Long id, Long hotelId, String name, BigDecimal price) {
        Hotel hotel = new Hotel();
        hotel.setId(hotelId);

        RoomType roomType = new RoomType();
        roomType.setId(id);
        roomType.setHotel(hotel);
        roomType.setName(name);
        roomType.setDescription("Nice room");
        roomType.setMaxAdults(2);
        roomType.setMaxChildren(1);
        roomType.setRoomSizeSqm(new BigDecimal("25.50"));
        roomType.setBasePrice(price);
        roomType.setCurrency("USD");
        roomType.setTotalRooms(10);
        roomType.setHasPrivateBathroom(true);
        roomType.setHasAirConditioning(true);
        roomType.setHasHeating(true);
        roomType.setHasBalcony(false);
        roomType.setHasTv(true);
        roomType.setHasMinibar(false);
        roomType.setHasSafe(true);
        roomType.setHasHairdryer(true);
        roomType.setHasWorkDesk(false);
        roomType.setHasSoundproofing(true);
        roomType.setHasCoffeeMachine(false);
        roomType.setCreatedAt(OffsetDateTime.parse("2026-03-15T10:00:00Z"));
        roomType.setUpdatedAt(OffsetDateTime.parse("2026-03-15T12:00:00Z"));

        return roomType;
    }


    @Test
    @DisplayName("POST /hotels/{hotelId}/room-types -> should create room type")
    void create_ShouldReturnCreatedRoomType() throws Exception {
        RoomType saved = buildRoomType(1L, 100L, "Standard", new BigDecimal("100.00"));

        given(service.create(eq(100L), any(RoomType.class))).willReturn(saved);

        String requestBody = """
                {
                  "name": "Standard",
                  "description": "Nice room",
                  "maxAdults": 2,
                  "maxChildren": 1,
                  "roomSizeSqm": 25.50,
                  "basePrice": 100.00,
                  "currency": "USD",
                  "totalRooms": 10,
                  "hasPrivateBathroom": true,
                  "hasAirConditioning": true,
                  "hasHeating": true,
                  "hasBalcony": false,
                  "hasTv": true,
                  "hasMinibar": false,
                  "hasSafe": true,
                  "hasHairdryer": true,
                  "hasWorkDesk": false,
                  "hasSoundproofing": true,
                  "hasCoffeeMachine": false
                }
                """;

        mockMvc.perform(post("/hotels/{hotelId}/room-types", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/hotels/100/room-types/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.hotelId").value(100))
                .andExpect(jsonPath("$.name").value("Standard"))
                .andExpect(jsonPath("$.basePrice").value(100.00));

        verify(service, times(1)).create(eq(100L), any(RoomType.class));
    }

    @Test
    @DisplayName("GET /hotels/{hotelId}/room-types/{id} -> should return one room type")
    void one_ShouldReturnRoomType() throws Exception {
        RoomType roomType = buildRoomType(1L, 100L, "Standard", new BigDecimal("100.00"));

        given(service.findById(100L, 1L)).willReturn(roomType);

        mockMvc.perform(get("/hotels/{hotelId}/room-types/{id}", 100L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.hotelId").value(100))
                .andExpect(jsonPath("$.name").value("Standard"))
                .andExpect(jsonPath("$.currency").value("USD"));

        verify(service, times(1)).findById(100L, 1L);
    }

    @Test
    @DisplayName("PUT /hotels/{hotelId}/room-types/{id} -> should replace room type")
    void replace_ShouldReturnUpdatedRoomType() throws Exception {
        RoomType updated = buildRoomType(1L, 100L, "Deluxe", new BigDecimal("180.00"));

        given(service.replace(eq(100L), eq(1L), any())).willReturn(updated);

        String requestBody = """
                {
                  "name": "Deluxe",
                  "description": "Updated room",
                  "maxAdults": 3,
                  "maxChildren": 2,
                  "roomSizeSqm": 35.00,
                  "basePrice": 180.00,
                  "currency": "USD",
                  "totalRooms": 8,
                  "hasPrivateBathroom": true,
                  "hasAirConditioning": true,
                  "hasHeating": true,
                  "hasBalcony": true,
                  "hasTv": true,
                  "hasMinibar": true,
                  "hasSafe": true,
                  "hasHairdryer": true,
                  "hasWorkDesk": true,
                  "hasSoundproofing": true,
                  "hasCoffeeMachine": true
                }
                """;

        mockMvc.perform(put("/hotels/{hotelId}/room-types/{id}", 100L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Deluxe"))
                .andExpect(jsonPath("$.basePrice").value(180.00));

        verify(service, times(1)).replace(eq(100L), eq(1L), any());
    }

    @Test
    @DisplayName("DELETE /hotels/{hotelId}/room-types/{id} -> should return 204")
    void delete_ShouldReturnNoContent() throws Exception {
        willDoNothing().given(service).delete(100L, 1L);

        mockMvc.perform(delete("/hotels/{hotelId}/room-types/{id}", 100L, 1L))
                .andExpect(status().isNoContent());

        verify(service, times(1)).delete(100L, 1L);
    }
}
