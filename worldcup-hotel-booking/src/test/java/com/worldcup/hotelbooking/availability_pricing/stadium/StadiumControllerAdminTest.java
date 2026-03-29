package com.worldcup.hotelbooking.availability_pricing.stadium;

import com.worldcup.hotelbooking.BaseIntegrationTest;
import com.worldcup.hotelbooking.auth.LoginRequest;
import com.worldcup.hotelbooking.auth.LoginResponse;
import com.worldcup.hotelbooking.tournament.match.Match;
import com.worldcup.hotelbooking.tournament.match.MatchRepository;
import com.worldcup.hotelbooking.security.RefreshTokenRepository;
import com.worldcup.hotelbooking.tournament.stadium.Stadium;
import com.worldcup.hotelbooking.tournament.stadium.StadiumRepository;
import com.worldcup.hotelbooking.user.AppUser;
import com.worldcup.hotelbooking.user.AppUserRepository;
import com.worldcup.hotelbooking.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StadiumControllerAdminTest extends BaseIntegrationTest {

    @Autowired
    private StadiumRepository stadiumRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private String adminToken;
    private String guestToken;
    private Long stadiumId;

    @BeforeEach
    void setUp() throws Exception {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        matchRepository.deleteAll();
        stadiumRepository.deleteAll();

        // Create admin
        AppUser admin = new AppUser();
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setEnabled(true);
        admin.setRoles(Set.of(Role.ADMIN));
        userRepository.save(admin);

        // Create guest
        AppUser guest = new AppUser();
        guest.setUsername("guest");
        guest.setEmail("guest@test.com");
        guest.setPassword(passwordEncoder.encode("guestpass"));
        guest.setEnabled(true);
        guest.setRoles(Set.of(Role.GUEST));
        userRepository.save(guest);

        adminToken = loginAndGetToken("admin", "adminpass");
        guestToken = loginAndGetToken("guest", "guestpass");

        // Create a stadium for tests
        Stadium stadium = new Stadium();
        stadium.setName("Test Stadium");
        stadium.setCity("City");
        stadium.setCapacity(50000);
        stadiumId = stadiumRepository.save(stadium).getId();
    }

    @Test
    void createStadium_withAdminToken_shouldReturn201() throws Exception {
        Stadium newStadium = new Stadium();
        newStadium.setName("New Stadium");
        newStadium.setCity("New City");
        newStadium.setCapacity(30000);

        mockMvc.perform(post("/stadiums")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStadium)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Stadium"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void createStadium_withGuestToken_shouldReturn403() throws Exception {
        Stadium newStadium = new Stadium();
        newStadium.setName("New Stadium");
        newStadium.setCity("New City");

        mockMvc.perform(post("/stadiums")
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStadium)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createStadium_withoutToken_shouldReturn401() throws Exception {
        Stadium newStadium = new Stadium();
        newStadium.setName("New Stadium");

        mockMvc.perform(post("/stadiums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newStadium)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateStadium_withAdminToken_shouldReturn200() throws Exception {
        Stadium updated = new Stadium();
        updated.setName("Updated Name");
        updated.setCity("Updated City");
        updated.setCapacity(60000);

        mockMvc.perform(put("/stadiums/{id}", stadiumId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void updateStadium_withGuestToken_shouldReturn403() throws Exception {
        Stadium updated = new Stadium();
        updated.setName("Updated Name");

        mockMvc.perform(put("/stadiums/{id}", stadiumId)
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteStadium_withAdminToken_noMatches_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/stadiums/{id}", stadiumId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/stadiums/{id}", stadiumId))
                .andExpect(status().isNotFound()); // assuming StadiumNotFoundException -> 404
    }

    @Test
    void deleteStadium_withAdminToken_hasMatches_shouldReturn400() throws Exception {
        // Create a match linked to this stadium
        Match match = new Match();
        match.setHomeTeam("Home");
        match.setAwayTeam("Away");
        match.setMatchDateTime(LocalDateTime.now());
        match.setStage(Match.MatchStage.GROUP_STAGE_1);
        match.setStadium(stadiumRepository.findById(stadiumId).orElseThrow());
        matchRepository.save(match);

        mockMvc.perform(delete("/stadiums/{id}", stadiumId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict()); // or 409 Conflict depending on your exception handler
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
        return response.accessToken();
    }
}