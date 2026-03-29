package com.worldcup.hotelbooking.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;

public record RegistrationResponseDto(
        @JsonProperty("ID") Long id,
        @JsonProperty("USERNAME") String username,
        @JsonProperty("EMAIL") String email,
        @JsonProperty("ROLES") Set<Role> roles,
        @JsonProperty("ENABLED") boolean enabled
) {}