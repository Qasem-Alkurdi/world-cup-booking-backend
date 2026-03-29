package com.worldcup.hotelbooking.user.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldcup.hotelbooking.auth.AuthService;
import com.worldcup.hotelbooking.security.JwtTokenService;
import com.worldcup.hotelbooking.security.RateLimitService;
import com.worldcup.hotelbooking.user.AppUserRequestDto;
import com.worldcup.hotelbooking.user.PasswordValidationException;
import com.worldcup.hotelbooking.user.RegistrationResponseDto;
import com.worldcup.hotelbooking.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "storage.upload-dir=./test-uploads")
class RegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    // No @MockitoBean for StorageProperties – the real bean is created and uses the property from @TestPropertySource

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturn201WhenRegistrationSucceeds() throws Exception {
        AppUserRequestDto dto = new AppUserRequestDto("newuser", "new@example.com", "StrongP@ss123");
        RegistrationResponseDto responseDto = new RegistrationResponseDto(1L, "newuser", "new@example.com", Set.of(Role.GUEST), true);

        when(authService.register(any(AppUserRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ID").value(1))
                .andExpect(jsonPath("$.USERNAME").value("newuser"))
                .andExpect(jsonPath("$.EMAIL").value("new@example.com"));
    }

    @Test
    void shouldReturn400WhenPasswordWeak() throws Exception {
        // Use a password that passes DTO length validation but is weak
        AppUserRequestDto dto = new AppUserRequestDto("testuser", "test@example.com", "weak123");
        when(authService.register(any(AppUserRequestDto.class)))
                .thenThrow(new PasswordValidationException(java.util.List.of("Too weak")));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Too weak"));
    }
}