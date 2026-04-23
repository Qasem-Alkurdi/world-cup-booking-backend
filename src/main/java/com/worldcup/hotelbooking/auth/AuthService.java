package com.worldcup.hotelbooking.auth;

import com.worldcup.hotelbooking.security.JwtTokenService;
import com.worldcup.hotelbooking.security.RefreshToken;
import com.worldcup.hotelbooking.security.RefreshTokenRepository;
import com.worldcup.hotelbooking.user.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenDays;
    private final ExternalProviderRepository externalProviderRepository;
    private final AppUserService appUserService;   // new

    public AuthService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService tokenService,
                       RefreshTokenRepository refreshTokenRepository,
                       @Value("${security.jwt.refresh-token-days}") long refreshTokenDays,
                       ExternalProviderRepository externalProviderRepository,
                       AppUserService appUserService) {   // added
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenDays = refreshTokenDays;
        this.externalProviderRepository = externalProviderRepository;
        this.appUserService = appUserService;
    }

    @Transactional
    public RegistrationResponseDto register(AppUserRequestDto dto) {
        AppUser createdUser = appUserService.createUser(dto);
        return AppUserMapper.toRegistrationDto(createdUser);
    }

    @Transactional()
    public LoginResponse login(String username, String password) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("User account is disabled");
        }

        List<String> roles = user.getRoles().stream()
                .map(role -> role.name().toUpperCase())
                .collect(Collectors.toList());
        String accessToken = tokenService.generateAccessToken(user.getUsername(), user.getId(), roles);

        // Generate and save refresh token
        String refreshTokenValue = tokenService.generateRefreshToken();
        Instant expiry = Instant.now().plusSeconds(refreshTokenDays * 24 * 60 * 60);
        RefreshToken refreshToken = new RefreshToken(refreshTokenValue, user, expiry);
        refreshTokenRepository.save(refreshToken);

        return new LoginResponse(accessToken, refreshTokenValue, tokenService.getAccessTokenExpiresInSeconds());
    }


    @Transactional
    public LoginResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.isExpired()) {
            throw new InvalidRefreshTokenException("Refresh token is revoked or expired");
        }

        AppUser user = refreshToken.getUser();
        if (!user.isEnabled()) {
            throw new InvalidRefreshTokenException("User account is disabled");
        }

        // Rotate refresh token: revoke old, create new
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Generate new refresh token
        String newRefreshTokenValue = tokenService.generateRefreshToken();
        Instant expiry = Instant.now().plusSeconds(refreshTokenDays * 24 * 60 * 60);
        RefreshToken newRefreshToken = new RefreshToken(newRefreshTokenValue, user, expiry);
        refreshTokenRepository.save(newRefreshToken);

        // Generate new access token
        List<String> roles = user.getRoles().stream()
                .map(role -> role.name().toUpperCase())
                .collect(Collectors.toList());
        String newAccessToken = tokenService.generateAccessToken(user.getUsername(), user.getId(), roles);

        return new LoginResponse(newAccessToken, newRefreshTokenValue, tokenService.getAccessTokenExpiresInSeconds());
    }

    @Transactional
    public void revokeRefreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refresh token not found"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public LoginResponse oauth2Login(String email, String name, String providerStr, String providerId) {
        Provider provider = Provider.valueOf(providerStr.toUpperCase());

        // 1. Check if external provider already linked
        Optional<ExternalProvider> existingProvider = externalProviderRepository
                .findByProviderAndProviderId(provider, providerId);
        if (existingProvider.isPresent()) {
            // User exists via provider
            AppUser user = existingProvider.get().getUser();
            return generateTokensForUser(user);
        }

        // 2. Try to find user by email
        Optional<AppUser> userByEmail = userRepository.findByEmail(email);
        AppUser user;
        if (userByEmail.isPresent()) {
            user = userByEmail.get();
        } else {
            // 3. Create new user
            user = new AppUser();
            user.setUsername(email);                  // or generate a unique username
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // random, unusable password
            user.setEnabled(true);
            user.setRoles(Set.of(Role.GUEST));
            user = userRepository.save(user);
        }

        // 4. Link the external provider to the user
        ExternalProvider externalProvider = new ExternalProvider(provider, providerId, user);
        externalProviderRepository.save(externalProvider);
        user.getExternalProviders().add(externalProvider);

        // 5. Generate tokens
        return generateTokensForUser(user);
    }

    private LoginResponse generateTokensForUser(AppUser user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.name().toUpperCase())
                .collect(Collectors.toList());

        String accessToken = tokenService.generateAccessToken(user.getUsername(), user.getId(), roles);

        String refreshTokenValue = tokenService.generateRefreshToken();
        Instant expiry = Instant.now().plusSeconds(refreshTokenDays * 24 * 60 * 60);
        RefreshToken refreshToken = new RefreshToken(refreshTokenValue, user, expiry);
        refreshTokenRepository.save(refreshToken);

        return new LoginResponse(accessToken, refreshTokenValue, tokenService.getAccessTokenExpiresInSeconds());
    }
}