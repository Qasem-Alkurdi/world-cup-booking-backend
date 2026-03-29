package com.worldcup.hotelbooking.auth;

import com.worldcup.hotelbooking.user.AppUserRequestDto;
import com.worldcup.hotelbooking.user.RegistrationResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and token management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<RegistrationResponseDto> register(
            @Valid @RequestBody AppUserRequestDto dto,
            UriComponentsBuilder uriBuilder) {

        RegistrationResponseDto created = authService.register(dto);

        return ResponseEntity
                .created(uriBuilder.path("/users/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return access & refresh tokens")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.username(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Obtain a new access token using a refresh token")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke")
    @Operation(summary = "Revoke a refresh token (logout)")
    public ResponseEntity<Void> revoke(@Valid @RequestBody RefreshTokenRequest request) {
        authService.revokeRefreshToken(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}