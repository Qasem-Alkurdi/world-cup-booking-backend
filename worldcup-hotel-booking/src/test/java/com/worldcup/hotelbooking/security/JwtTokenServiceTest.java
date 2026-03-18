package com.worldcup.hotelbooking.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    private JwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new JwtTokenService(jwtEncoder, "test-issuer", 15);
        ReflectionTestUtils.setField(tokenService, "refreshTokenDays", 7L);
    }

    @Test
    void generateAccessToken() {
        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("encoded-jwt");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        String token = tokenService.generateAccessToken("user", 123L, List.of("ROLE_USER"));

        assertThat(token).isEqualTo("encoded-jwt");
        // Additional verification: we could capture the claims and inspect them,
        // but that would require more complex mocking. Simpler to just verify call.
    }

    @Test
    void generateRefreshToken() {
        String token = tokenService.generateRefreshToken();
        assertThat(token).isNotBlank();
        assertThat(token.length()).isEqualTo(36); // UUID length
    }

    @Test
    void getAccessTokenExpiresInSeconds() {
        assertThat(tokenService.getAccessTokenExpiresInSeconds()).isEqualTo(15 * 60);
    }

    @Test
    void getRefreshTokenExpiryInSeconds() {
        assertThat(tokenService.getRefreshTokenExpiryInSeconds()).isEqualTo(7 * 24 * 60 * 60);
    }
}