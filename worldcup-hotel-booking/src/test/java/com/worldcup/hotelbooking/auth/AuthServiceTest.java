package com.worldcup.hotelbooking.auth;

import com.worldcup.hotelbooking.security.JwtTokenService;
import com.worldcup.hotelbooking.security.RefreshToken;
import com.worldcup.hotelbooking.security.RefreshTokenRepository;
import com.worldcup.hotelbooking.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService tokenService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private ExternalProviderRepository externalProviderRepository;

    private AuthService authService;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private AppUser testUser;
    private final String rawPassword = "password";
    private final String encodedPassword = "encoded";
    private AppUserService appUserServiceMock;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword(encodedPassword);
        testUser.setEnabled(true);
        testUser.setRoles(Set.of(Role.GUEST));

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                tokenService,
                refreshTokenRepository,
                7L,
                externalProviderRepository,
                appUserServiceMock
        );
    }

    @Test
    void login_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(tokenService.generateAccessToken(eq("testuser"), any(), anyList()))   // changed anyLong() -> any()
                .thenReturn("access-token");
        when(tokenService.generateRefreshToken()).thenReturn("refresh-token");
        when(tokenService.getAccessTokenExpiresInSeconds()).thenReturn(900L);

        LoginResponse response = authService.login("testuser", rawPassword);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.expiresInSeconds()).isEqualTo(900L);

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken savedToken = refreshTokenCaptor.getValue();
        assertThat(savedToken.getToken()).isEqualTo("refresh-token");
        assertThat(savedToken.getUser()).isEqualTo(testUser);
        assertThat(savedToken.isRevoked()).isFalse();
    }

    @Test
    void login_invalidPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", encodedPassword)).thenReturn(false);

        assertThatThrownBy(() -> authService.login("testuser", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_userNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("unknown", "any"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_disabledUser() {
        testUser.setEnabled(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        // Need to stub password match to true so that we reach the enabled check
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        assertThatThrownBy(() -> authService.login("testuser", rawPassword))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("User account is disabled");
    }

    @Test
    void refresh_validToken() {
        String oldTokenValue = "old-refresh";
        RefreshToken oldToken = new RefreshToken(oldTokenValue, testUser, Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByToken(oldTokenValue)).thenReturn(Optional.of(oldToken));

        when(tokenService.generateRefreshToken()).thenReturn("new-refresh");
        when(tokenService.generateAccessToken(eq("testuser"), any(), anyList()))   // anyLong() -> any()
                .thenReturn("new-access");
        when(tokenService.getAccessTokenExpiresInSeconds()).thenReturn(900L);

        LoginResponse response = authService.refresh(oldTokenValue);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");

        assertThat(oldToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(oldToken);
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_revokedToken() {
        String tokenValue = "revoked";
        RefreshToken token = new RefreshToken(tokenValue, testUser, Instant.now().plusSeconds(3600));
        token.setRevoked(true);
        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh(tokenValue))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Refresh token is revoked or expired");
    }

    @Test
    void refresh_expiredToken() {
        String tokenValue = "expired";
        RefreshToken token = new RefreshToken(tokenValue, testUser, Instant.now().minusSeconds(3600));
        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh(tokenValue))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_userDisabled() {
        testUser.setEnabled(false);
        String tokenValue = "token";
        RefreshToken token = new RefreshToken(tokenValue, testUser, Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh(tokenValue))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("User account is disabled");
    }

    @Test
    void revokeRefreshToken_valid() {
        String tokenValue = "token";
        RefreshToken token = new RefreshToken(tokenValue, testUser, Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

        authService.revokeRefreshToken(tokenValue);

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revokeRefreshToken_notFound() {
        when(refreshTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.revokeRefreshToken("invalid"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Refresh token not found");
    }

    @Test
    void oauth2Login_existingProvider() {
        String email = "user@example.com";
        String name = "User";
        String provider = "GOOGLE";
        String providerId = "google123";

        ExternalProvider externalProvider = new ExternalProvider(Provider.GOOGLE, providerId, testUser);
        when(externalProviderRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId))
                .thenReturn(Optional.of(externalProvider));

        when(tokenService.generateAccessToken(anyString(), any(), anyList()))   // anyLong() -> any()
                .thenReturn("access");
        when(tokenService.generateRefreshToken()).thenReturn("refresh");
        when(tokenService.getAccessTokenExpiresInSeconds()).thenReturn(900L);

        LoginResponse response = authService.oauth2Login(email, name, provider, providerId);

        assertThat(response.accessToken()).isEqualTo("access");
        verify(userRepository, never()).save(any());
    }

    @Test
    void oauth2Login_userExistsByEmail() {
        String email = "user@example.com";
        String provider = "GOOGLE";
        String providerId = "google123";

        when(externalProviderRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        when(tokenService.generateAccessToken(anyString(), any(), anyList()))   // anyLong() -> any()
                .thenReturn("access");
        when(tokenService.generateRefreshToken()).thenReturn("refresh");

        LoginResponse response = authService.oauth2Login(email, "User", provider, providerId);

        assertThat(response.accessToken()).isEqualTo("access");
        verify(externalProviderRepository).save(any(ExternalProvider.class));
    }

    @Test
    void oauth2Login_newUser() {
        String email = "new@example.com";
        String provider = "GOOGLE";
        String providerId = "google123";

        when(externalProviderRepository.findByProviderAndProviderId(Provider.GOOGLE, providerId))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("randomEncoded");

        when(tokenService.generateAccessToken(anyString(), any(), anyList()))   // anyLong() -> any()
                .thenReturn("access");
        when(tokenService.generateRefreshToken()).thenReturn("refresh");

        LoginResponse response = authService.oauth2Login(email, "New User", provider, providerId);

        assertThat(response.accessToken()).isEqualTo("access");
        verify(userRepository).save(any(AppUser.class));
        verify(externalProviderRepository).save(any(ExternalProvider.class));
    }
}