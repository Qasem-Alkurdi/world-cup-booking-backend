package com.worldcup.hotelbooking.security;

import com.worldcup.hotelbooking.auth.AuthService;
import com.worldcup.hotelbooking.auth.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(@Lazy AuthService authService,
                                              @Value("${app.frontend-url}") String frontendUrl) {
        this.authService = authService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        String providerId = oauth2User.getAttribute("sub");          // Google user ID
        String provider = oauthToken.getAuthorizedClientRegistrationId(); // "google"

        LoginResponse loginResponse = authService.oauth2Login(email, name, provider, providerId);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .queryParam("access_token", loginResponse.accessToken())
                .queryParam("refresh_token", loginResponse.refreshToken())
                .queryParam("expires_in", loginResponse.expiresInSeconds())
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }
}