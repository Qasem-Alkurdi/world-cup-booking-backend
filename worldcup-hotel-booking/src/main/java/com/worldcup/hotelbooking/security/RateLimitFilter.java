package com.worldcup.hotelbooking.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import jakarta.servlet.Filter;

@Component
public class RateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;
    private final JwtTokenService tokenService;

    public RateLimitFilter(RateLimitService rateLimitService,
                           JwtTokenService tokenService) {
        this.rateLimitService = rateLimitService;
        this.tokenService = tokenService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;


        String endpoint = httpRequest.getRequestURI();


        if (endpoint.contains("/availability") || endpoint.contains("/bookings") || endpoint.contains("/payments")) {

            String key = resolveKey(httpRequest, endpoint);

            Bucket bucket = rateLimitService.resolveBucket(key);

            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
               throw new RateLimitExceededException();
            }

        } else {
            chain.doFilter(request, response);
        }
    }

    private String resolveKey(HttpServletRequest request, String endpoint) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                Long userId = tokenService.extractUserId(token);

                return "USER_" + userId + "_" + endpoint;

            } catch (Exception e) {
                // If token is invalid, fall back to IP-based key
                return "IP_" + request.getRemoteAddr() + "_" + endpoint;
            }
        }

        //If the user is not looged in, use IP address as key
        return "IP_" + request.getRemoteAddr() + "_" + endpoint;
    }

}
