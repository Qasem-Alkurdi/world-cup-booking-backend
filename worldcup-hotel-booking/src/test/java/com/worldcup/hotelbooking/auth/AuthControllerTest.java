package com.worldcup.hotelbooking.auth;

import com.worldcup.hotelbooking.BaseIntegrationTest;
import com.worldcup.hotelbooking.security.RefreshTokenRepository;
import com.worldcup.hotelbooking.user.AppUser;
import com.worldcup.hotelbooking.user.AppUserRepository;
import com.worldcup.hotelbooking.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();   // <-- delete refresh tokens first
        userRepository.deleteAll();
    }

    @Test
    void login_withValidCredentials_shouldReturnTokens() throws Exception {
        createUser("testuser", "password", Role.GUEST);

        LoginRequest request = new LoginRequest("testuser", "password");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").exists())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertThat(json).contains("accessToken");
    }

    @Test
    void login_withInvalidPassword_shouldReturn401() throws Exception {
        createUser("testuser", "password", Role.GUEST);
        LoginRequest request = new LoginRequest("testuser", "wrong");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void login_withNonExistentUser_shouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("unknown", "any");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void login_withDisabledUser_shouldReturn401() throws Exception {
        AppUser disabled = new AppUser();
        disabled.setUsername("disabled");
        disabled.setEmail("disabled@test.com");   // <-- add this line
        disabled.setPassword(passwordEncoder.encode("pass"));
        disabled.setEnabled(false);
        disabled.setRoles(Set.of(Role.GUEST));
        userRepository.save(disabled);

        LoginRequest request = new LoginRequest("disabled", "pass");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    private void createUser(String username, String rawPassword, Role... roles) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        user.setRoles(Set.of(roles));
        userRepository.save(user);
    }



    @Test
    void refresh_withValidToken_shouldReturnNewTokens() throws Exception {
        createUser("refreshuser", "pass", Role.GUEST);
        String refreshToken = loginAndGetRefreshToken("refreshuser", "pass");

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        LoginResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
        assertThat(response.refreshToken()).isNotEqualTo(refreshToken); // token rotated
    }

    @Test
    void refresh_withRevokedToken_shouldReturn401() throws Exception {
        createUser("revokeduser", "pass", Role.GUEST);
        String refreshToken = loginAndGetRefreshToken("revokeduser", "pass");

        // Revoke it
        RefreshTokenRequest revokeRequest = new RefreshTokenRequest(refreshToken);
        mockMvc.perform(post("/auth/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokeRequest)))
                .andExpect(status().isNoContent());

        // Try to use it again
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokeRequest)))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndGetRefreshToken(String username, String password) throws Exception {
        LoginRequest loginReq = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
        return response.refreshToken();
    }

    @Test
    void login_withBlankUsername_shouldReturn400() throws Exception {
        LoginRequest request = new LoginRequest("", "password");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withBlankPassword_shouldReturn400() throws Exception {
        LoginRequest request = new LoginRequest("user", "");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_withBlankToken_shouldReturn400() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("");
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}