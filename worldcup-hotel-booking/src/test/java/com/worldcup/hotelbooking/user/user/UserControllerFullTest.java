package com.worldcup.hotelbooking.user.user;

import com.worldcup.hotelbooking.BaseIntegrationTest;
import com.worldcup.hotelbooking.auth.LoginRequest;
import com.worldcup.hotelbooking.auth.LoginResponse;
import com.worldcup.hotelbooking.booking.booking.BookingRepository;
import com.worldcup.hotelbooking.notification.notification.NotificationService;
import com.worldcup.hotelbooking.security.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerFullTest extends BaseIntegrationTest {

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private BookingRepository bookingRepository;

    // Mock the NotificationService to prevent async database calls in tests
    @MockitoBean
    private NotificationService notificationService;

    private String adminToken;
    private String guestToken;
    private Long adminId;
    private Long guestId;

    @BeforeEach
    void setUp() throws Exception {
        refreshTokenRepository.deleteAll();
        bookingRepository.deleteAll();
        userRepository.deleteAll();

        // Create admin
        AppUser admin = new AppUser();
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setEnabled(true);
        admin.setRoles(Set.of(Role.ADMIN));
        adminId = userRepository.save(admin).getId();

        // Create guest
        AppUser guest = new AppUser();
        guest.setUsername("guest");
        guest.setEmail("guest@test.com");
        guest.setPassword(passwordEncoder.encode("guestpass"));
        guest.setEnabled(true);
        guest.setRoles(Set.of(Role.GUEST));
        guestId = userRepository.save(guest).getId();

        adminToken = loginAndGetToken("admin", "adminpass");
        guestToken = loginAndGetToken("guest", "guestpass");
    }

    // ---------- Public Registration ----------
    @Test
    void registerUser_valid_shouldReturn201() throws Exception {
        AppUserRequestDto dto = new AppUserRequestDto("newuser", "new@example.com", "password123");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.USERNAME").value("newuser"))          // uppercase
                .andExpect(jsonPath("$.EMAIL").value("new@example.com"))    // uppercase
                .andExpect(jsonPath("$.ROLES[0]").value("GUEST"))           // uppercase
                .andExpect(jsonPath("$.ENABLED").value(true));              // uppercase
    }

    @Test
    void registerUser_invalidDto_shouldReturn400() throws Exception {
        AppUserRequestDto dto = new AppUserRequestDto("", "invalid-email", "short");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ---------- GET /users (admin only) ----------
    @Test
    void getAllUsers_withAdminToken_shouldReturn200() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getAllUsers_withGuestToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/users")
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isForbidden());
    }

    // ---------- GET /users/{id} (self or admin) ----------
    @Test
    void getUserById_asSelf_shouldReturn200() throws Exception {
        mockMvc.perform(get("/users/{id}", guestId)
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ID").value(guestId));                // uppercase
    }

    @Test
    void getUserById_asAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(get("/users/{id}", guestId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void getUserById_asDifferentUser_shouldReturn403() throws Exception {
        // guest tries to access admin's profile
        mockMvc.perform(get("/users/{id}", adminId)
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isForbidden());
    }

    // ---------- PUT /users/{id} (full update) ----------
    @Test
    void updateUser_asSelf_shouldReturn200() throws Exception {
        AppUserRequestDto updateDto = new AppUserRequestDto("updatedGuest", "updated@guest.com", "newpass");

        mockMvc.perform(put("/users/{id}", guestId)
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.USERNAME").value("updatedGuest"))   // uppercase
                .andExpect(jsonPath("$.EMAIL").value("updated@guest.com")); // uppercase
    }

    // ---------- PATCH /users/{id} (partial update) ----------
    @Test
    void partialUpdateUser_asSelf_shouldReturn200() throws Exception {
        Map<String, Object> updates = Map.of("username", "partialGuest");

        mockMvc.perform(patch("/users/{id}", guestId)
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.USERNAME").value("partialGuest"));  // uppercase
    }

    @Test
    void partialUpdateUser_withInvalidField_shouldReturn400() throws Exception {
        Map<String, Object> updates = Map.of("invalid", "value");

        mockMvc.perform(patch("/users/{id}", guestId)
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isBadRequest());
    }

    // ---------- DELETE /users/{id} ----------
    @Test
    void deleteUser_asSelf_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/users/{id}", guestId)
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isNoContent());

        // Verify user no longer exists
        mockMvc.perform(get("/users/{id}", guestId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_asAdmin_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/users/{id}", guestId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_asDifferentUser_shouldReturn403() throws Exception {
        // guest tries to delete admin
        mockMvc.perform(delete("/users/{id}", adminId)
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isForbidden());
    }

    // ---------- GET /users/{id}/bookings ----------
    @Test
    void getUserBookings_asSelf_noBookings_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/users/{id}/bookings", guestId)
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------- GET /users/search (admin only) ----------
    @Test
    void searchUsers_asAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(get("/users/search")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].USERNAME").value("admin"));       // uppercase
    }

    @Test
    void searchUsers_asGuest_shouldReturn403() throws Exception {
        mockMvc.perform(get("/users/search")
                        .header("Authorization", "Bearer " + guestToken))
                .andExpect(status().isForbidden());
    }

    // ---------- PUT /users/{id}/roles (admin only) ----------
    @Test
    void updateUserRoles_asAdmin_shouldReturn200() throws Exception {
        UserRoleUpdateDto rolesDto = new UserRoleUpdateDto(Set.of(Role.ADMIN, Role.MANAGER));

        mockMvc.perform(put("/users/{id}/roles", guestId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolesDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ROLES").isArray())                  // uppercase
                .andExpect(jsonPath("$.ROLES.length()").value(2));         // uppercase
    }

    @Test
    void updateUserRoles_asGuest_shouldReturn403() throws Exception {
        UserRoleUpdateDto rolesDto = new UserRoleUpdateDto(Set.of(Role.ADMIN));

        mockMvc.perform(put("/users/{id}/roles", guestId)
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolesDto)))
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