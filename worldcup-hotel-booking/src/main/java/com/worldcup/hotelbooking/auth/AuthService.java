package com.worldcup.hotelbooking.auth;

import com.worldcup.hotelbooking.security.RefreshToken;
import com.worldcup.hotelbooking.security.RefreshTokenRepository;
import com.worldcup.hotelbooking.user.user.AppUser;
import com.worldcup.hotelbooking.user.user.AppUserRepository;
import com.worldcup.hotelbooking.security.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenDays;

    public AuthService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService tokenService,
                       RefreshTokenRepository refreshTokenRepository,
                       @Value("${security.jwt.refresh-token-days}") long refreshTokenDays) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional(readOnly = true)
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
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
}