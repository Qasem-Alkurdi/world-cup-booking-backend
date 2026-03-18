package com.worldcup.hotelbooking.availability_pricing.match;

import com.worldcup.hotelbooking.BaseIntegrationTest;
import com.worldcup.hotelbooking.availability_pricing.stadium.Stadium;
import com.worldcup.hotelbooking.availability_pricing.stadium.StadiumRepository;
import com.worldcup.hotelbooking.auth.LoginRequest;
import com.worldcup.hotelbooking.auth.LoginResponse;
import com.worldcup.hotelbooking.security.RefreshTokenRepository;
import com.worldcup.hotelbooking.user.user.AppUser;
import com.worldcup.hotelbooking.user.user.AppUserRepository;
import com.worldcup.hotelbooking.user.user.Role;
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

class MatchControllerTest extends BaseIntegrationTest {

    @Autowired
    private StadiumRepository stadiumRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private Long stadiumId;
    private Long matchId;

    @BeforeEach
    void setUp() {
        // Clean up
        matchRepository.deleteAll();
        stadiumRepository.deleteAll();

        // Create a stadium
        Stadium stadium = new Stadium();
        stadium.setName("Test Stadium");
        stadium.setCity("Test City");
        stadium.setLatitude(25.0);
        stadium.setLongitude(51.0);
        stadium.setCapacity(50000);
        stadiumId = stadiumRepository.save(stadium).getId();

        // Create a match
        Match match = new Match();
        match.setHomeTeam("Team A");
        match.setAwayTeam("Team B");
        match.setMatchDateTime(LocalDateTime.now().plusDays(10));
        match.setStage(Match.MatchStage.GROUP_STAGE_1);
        match.setStadium(stadium);
        match.setOpeningMatch(false);
        match.setDerby(false);
        matchId = matchRepository.save(match).getId();
    }

    @BeforeEach
    void setUpUsers() {
        refreshTokenRepository.deleteAll();
        appUserRepository.deleteAll();
        // Delete users if needed (careful with foreign keys – delete refresh tokens first)
        // For simplicity, we'll just create if they don't exist.
        if (appUserRepository.findByUsername("admin").isEmpty()) {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setEmail("admin@test.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setEnabled(true);
            admin.setRoles(Set.of(Role.ADMIN));
            appUserRepository.save(admin);
        }
        if (appUserRepository.findByUsername("guest").isEmpty()) {
            AppUser guest = new AppUser();
            guest.setUsername("guest");
            guest.setEmail("guest@test.com");
            guest.setPassword(passwordEncoder.encode("Guest@123"));
            guest.setEnabled(true);
            guest.setRoles(Set.of(Role.GUEST));
            appUserRepository.save(guest);
        }
    }

    // ========== Public Endpoints (no token required) ==========

    @Test
    void getAllMatches_public_shouldReturn200() throws Exception {
        mockMvc.perform(get("/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(matchId))
                .andExpect(jsonPath("$[0].homeTeam").value("Team A"));
    }

    @Test
    void getMatchById_public_shouldReturn200() throws Exception {
        mockMvc.perform(get("/matches/{id}", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(matchId))
                .andExpect(jsonPath("$.homeTeam").value("Team A"));
    }

    @Test
    void getMatchesByStadium_public_shouldReturn200() throws Exception {
        mockMvc.perform(get("/matches/stadium/{stadiumId}", stadiumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(matchId));
    }

    @Test
    void getMatchesBetweenDates_public_shouldReturn200() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(20);

        mockMvc.perform(get("/matches/between")
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(matchId));
    }

    // ========== Admin Write Endpoints ==========

    @Test
    void createMatch_withAdminToken_shouldReturn201() throws Exception {
        String adminToken = loginAndGetToken("admin", "Admin@123");

        Match newMatch = new Match();
        newMatch.setHomeTeam("New Home");
        newMatch.setAwayTeam("New Away");
        newMatch.setMatchDateTime(LocalDateTime.now().plusMonths(1));
        newMatch.setStage(Match.MatchStage.QUARTER_FINAL);
        newMatch.setStadium(stadiumRepository.findById(stadiumId).orElseThrow()); // use existing stadium

        mockMvc.perform(post("/matches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMatch)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.homeTeam").value("New Home"))
                .andExpect(jsonPath("$.stadium.id").value(stadiumId));
    }

    @Test
    void createMatch_withGuestToken_shouldReturn403() throws Exception {
        String guestToken = loginAndGetToken("guest", "Guest@123");

        Match newMatch = new Match();
        newMatch.setHomeTeam("New Home");
        newMatch.setAwayTeam("New Away");
        newMatch.setMatchDateTime(LocalDateTime.now().plusMonths(1));
        newMatch.setStage(Match.MatchStage.QUARTER_FINAL);
        newMatch.setStadium(stadiumRepository.findById(stadiumId).orElseThrow());

        mockMvc.perform(post("/matches")
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMatch)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createMatch_withoutToken_shouldReturn401() throws Exception {
        Match newMatch = new Match();
        newMatch.setHomeTeam("New Home");
        newMatch.setAwayTeam("New Away");
        newMatch.setMatchDateTime(LocalDateTime.now().plusMonths(1));
        newMatch.setStage(Match.MatchStage.QUARTER_FINAL);
        newMatch.setStadium(stadiumRepository.findById(stadiumId).orElseThrow());

        mockMvc.perform(post("/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMatch)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMatch_withAdminToken_shouldReturn200() throws Exception {
        String adminToken = loginAndGetToken("admin", "Admin@123");

        Match updatedMatch = new Match();
        updatedMatch.setHomeTeam("Updated Home");
        updatedMatch.setAwayTeam("Updated Away");
        updatedMatch.setMatchDateTime(LocalDateTime.now().plusMonths(2));
        updatedMatch.setStage(Match.MatchStage.SEMI_FINAL);
        updatedMatch.setStadium(stadiumRepository.findById(stadiumId).orElseThrow());

        mockMvc.perform(put("/matches/{id}", matchId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedMatch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeTeam").value("Updated Home"));
    }

    @Test
    void updateMatch_withGuestToken_shouldReturn403() throws Exception {
        String guestToken = loginAndGetToken("guest", "Guest@123");

        Match updatedMatch = new Match();
        updatedMatch.setHomeTeam("Updated Home");
        updatedMatch.setAwayTeam("Updated Away");
        updatedMatch.setMatchDateTime(LocalDateTime.now().plusMonths(2));
        updatedMatch.setStage(Match.MatchStage.SEMI_FINAL);
        updatedMatch.setStadium(stadiumRepository.findById(stadiumId).orElseThrow());

        mockMvc.perform(put("/matches/{id}", matchId)
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedMatch)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteMatch_withAdminToken_shouldReturn204() throws Exception {
        String adminToken = loginAndGetToken("admin", "Admin@123");

        mockMvc.perform(delete("/matches/{id}", matchId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/matches/{id}", matchId))
                .andExpect(status().isNotFound()); // assuming your service throws MatchNotFoundException -> 404
    }

    @Test
    void deleteMatch_withGuestToken_shouldReturn403() throws Exception {
        String guestToken = loginAndGetToken("guest", "Guest@123");

        mockMvc.perform(delete("/matches/{id}", matchId)
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isForbidden());
    }

    // ========== Helper ==========

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