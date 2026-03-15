package com.worldcup.hotelbooking.user.user;

import com.worldcup.hotelbooking.BaseIntegrationTest;
import com.worldcup.hotelbooking.auth.LoginRequest;
import com.worldcup.hotelbooking.auth.LoginResponse;
import com.worldcup.hotelbooking.security.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerSecurityTest extends BaseIntegrationTest {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;



    private Long adminId;
    private Long guestId;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();   // <-- delete refresh tokens first
        userRepository.deleteAll();

        AppUser admin = new AppUser();
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("admin"));
        admin.setEnabled(true);
        admin.setRoles(Set.of(Role.ADMIN));
        adminId = userRepository.save(admin).getId();

        AppUser guest = new AppUser();
        guest.setUsername("guest");
        guest.setEmail("guest@test.com");
        guest.setPassword(passwordEncoder.encode("guest"));
        guest.setEnabled(true);
        guest.setRoles(Set.of(Role.GUEST));
        guestId = userRepository.save(guest).getId();
    }

    @Test
    void getUsers_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUsers_withAdminToken_shouldReturn200() throws Exception {
        String token = loginAndGetToken("admin", "admin");
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getUsers_withGuestToken_shouldReturn403() throws Exception {
        String token = loginAndGetToken("guest", "guest");
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_withOwnId_shouldReturn200() throws Exception {
        String token = loginAndGetToken("guest", "guest");
        mockMvc.perform(get("/users/{id}", guestId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getUserById_withDifferentUserId_shouldReturn403() throws Exception {
        String token = loginAndGetToken("guest", "guest");
        // Try to access admin's profile
        mockMvc.perform(get("/users/{id}", adminId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
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