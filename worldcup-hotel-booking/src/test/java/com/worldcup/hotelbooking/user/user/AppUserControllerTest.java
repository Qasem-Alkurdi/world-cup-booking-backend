package com.worldcup.hotelbooking.user.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.hotelbooking.booking.booking.BookingResponseDto;
import com.worldcup.hotelbooking.security.JwtTokenService;
import com.worldcup.hotelbooking.security.RateLimitService;
import com.worldcup.hotelbooking.user.AppUser;
import com.worldcup.hotelbooking.user.AppUserService;
import com.worldcup.hotelbooking.user.Role;
import com.worldcup.hotelbooking.user.UserRoleUpdateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "app.storage.upload-dir=./test-uploads")
class AppUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService appUserService;

    // REMOVED @MockitoBean StorageProperties — let the real bean read the property

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AppUser sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new AppUser();
        sampleUser.setId(1L);
        sampleUser.setUsername("john_doe");
        sampleUser.setEmail("john@example.com");
        sampleUser.setRoles(Set.of(Role.GUEST));
        sampleUser.setEnabled(true);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserById_asAdmin_returnsUser() throws Exception {
        when(appUserService.getUserById(1L)).thenReturn(sampleUser);
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.USERNAME").value("john_doe"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_paginated_returnsPage() throws Exception {
        Page<AppUser> page = new PageImpl<>(List.of(sampleUser), PageRequest.of(0, 20), 1);
        when(appUserService.getAllUsers(any(Pageable.class))).thenReturn(page);
        mockMvc.perform(get("/users").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].USERNAME").value("john_doe"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserByEmail_returnsUser() throws Exception {
        when(appUserService.getUserByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        mockMvc.perform(get("/users/email/john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.USERNAME").value("john_doe"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUserRoles_returnsUpdated() throws Exception {
        UserRoleUpdateDto roleUpdate = new UserRoleUpdateDto(Set.of(Role.ADMIN));
        AppUser updatedUser = sampleUser;
        updatedUser.setRoles(Set.of(Role.ADMIN));
        when(appUserService.updateUserRoles(eq(1L), anySet())).thenReturn(updatedUser);
        mockMvc.perform(put("/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ROLES[0]").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchUsers_returnsList() throws Exception {
        when(appUserService.searchUsers(eq("john"), isNull())).thenReturn(List.of(sampleUser));
        mockMvc.perform(get("/users/search").param("username", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].USERNAME").value("john_doe"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void partialUpdateUser_returnsUpdated() throws Exception {
        Map<String, Object> updates = Map.of("username", "newName");
        AppUser updated = sampleUser;
        updated.setUsername("newName");
        when(appUserService.partialUpdateUser(eq(1L), anyMap())).thenReturn(updated);
        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.USERNAME").value("newName"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/users/1")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUserBookings_returnsList() throws Exception {
        when(appUserService.getUserBookings(1L)).thenReturn(
                List.of(new BookingResponseDto(
                        100L,
                        null,
                        null,
                        0,
                        0,
                        0,
                        null,
                        null,
                        null,
                        null,
                        false,
                        new ArrayList<>()
                ))
        );
        mockMvc.perform(get("/users/1/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}