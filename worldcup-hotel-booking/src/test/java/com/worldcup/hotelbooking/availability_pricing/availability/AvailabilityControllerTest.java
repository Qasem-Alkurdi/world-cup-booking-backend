package com.worldcup.hotelbooking.availability_pricing.availability;


import com.worldcup.hotelbooking.booking.booking.BookingController;
import com.worldcup.hotelbooking.catalog.storage.StaticResourceConfig;
import com.worldcup.hotelbooking.catalog.storage.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AvailabilityController.class,
        excludeAutoConfiguration = {StaticResourceConfig.class})
@Import(AvailabilityControllerTest.TestBeans.class)
@AutoConfigureMockMvc(addFilters = false)
class AvailabilityControllerTest {
    @TestConfiguration
    static class TestBeans {
        @Bean
        StorageProperties storageProperties() {
            return new StorageProperties();
        }
    }
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvailabilityServiceImpl availabilityService;

    @Test
    void checkRoomTypeAvailability_shouldReturnTrue_whenServiceReturnsTrue() throws Exception {
        // Arrange
        long roomTypeId = 1L;
        String checkIn = "2026-07-01";
        String checkOut = "2026-07-05";

        when(availabilityService.checkRoomTypeAvailability(roomTypeId,
                java.time.LocalDate.parse(checkIn),
                java.time.LocalDate.parse(checkOut)))
                .thenReturn(true);

        // Act + Assert
        mockMvc.perform(get("/availability/room-type/{id}", roomTypeId)
                        .param("checkIn", checkIn)
                        .param("checkOut", checkOut))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(availabilityService).checkRoomTypeAvailability(
                roomTypeId,
                java.time.LocalDate.parse(checkIn),
                java.time.LocalDate.parse(checkOut)
        );
    }

    @Test
    void checkRoomTypeAvailability_shouldReturnFalse_whenServiceReturnsFalse() throws Exception {
        // Arrange
        long roomTypeId = 2L;
        String checkIn = "2026-08-10";
        String checkOut = "2026-08-12";

        when(availabilityService.checkRoomTypeAvailability(roomTypeId,
                java.time.LocalDate.parse(checkIn),
                java.time.LocalDate.parse(checkOut)))
                .thenReturn(false);

        // Act + Assert
        mockMvc.perform(get("/availability/room-type/{id}", roomTypeId)
                        .param("checkIn", checkIn)
                        .param("checkOut", checkOut))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(availabilityService).checkRoomTypeAvailability(
                roomTypeId,
                java.time.LocalDate.parse(checkIn),
                java.time.LocalDate.parse(checkOut)
        );
    }

    @Test
    void getAvailableRooms_shouldReturnAvailableRoomsCount() throws Exception {
        // Arrange
        long roomTypeId = 3L;
        String checkIn = "2026-09-01";
        String checkOut = "2026-09-04";

        when(availabilityService.getAvailableRooms(roomTypeId,
                java.time.LocalDate.parse(checkIn),
                java.time.LocalDate.parse(checkOut)))
                .thenReturn(7);

        // Act + Assert
        mockMvc.perform(get("/availability/room-type/{id}/rooms", roomTypeId)
                        .param("checkIn", checkIn)
                        .param("checkOut", checkOut))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));

        verify(availabilityService).getAvailableRooms(
                roomTypeId,
                java.time.LocalDate.parse(checkIn),
                java.time.LocalDate.parse(checkOut)
        );
    }
}

