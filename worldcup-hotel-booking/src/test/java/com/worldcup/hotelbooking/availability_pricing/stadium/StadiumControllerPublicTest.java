package com.worldcup.hotelbooking.availability_pricing.stadium;

import com.worldcup.hotelbooking.BaseIntegrationTest;
import com.worldcup.hotelbooking.tournament.stadium.Stadium;
import com.worldcup.hotelbooking.tournament.stadium.StadiumRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StadiumControllerPublicTest extends BaseIntegrationTest {

    @Autowired
    private StadiumRepository stadiumRepository;

    private Long stadiumId;

    @BeforeEach
    void setUp() {
        stadiumRepository.deleteAll();
        Stadium stadium = new Stadium();
        stadium.setName("Test Stadium");
        stadium.setCity("Test City");
        stadium.setLatitude(25.0);
        stadium.setLongitude(51.0);
        stadium.setCapacity(50000);
        stadiumId = stadiumRepository.save(stadium).getId();
    }

    @Test
    void getStadiums_withoutToken_shouldReturn200() throws Exception {
        mockMvc.perform(get("/stadiums"))
                .andExpect(status().isOk());
    }

    @Test
    void getStadiumById_withoutToken_shouldReturn200() throws Exception {
        mockMvc.perform(get("/stadiums/{id}", stadiumId))
                .andExpect(status().isOk());
    }
}