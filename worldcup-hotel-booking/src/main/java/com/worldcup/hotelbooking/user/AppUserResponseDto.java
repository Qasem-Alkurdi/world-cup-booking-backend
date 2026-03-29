package com.worldcup.hotelbooking.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.worldcup.hotelbooking.booking.booking.BookingResponseDto;

import java.util.List;
import java.util.Set;


public record AppUserResponseDto(Long id, String username, String email, Set<Role> roles, boolean enabled,
                                 List<BookingResponseDto> bookings) {

    public AppUserResponseDto(
            @JsonProperty("ID") Long id,
            @JsonProperty("USERNAME") String username,
            @JsonProperty("EMAIL") String email,
            @JsonProperty("ROLES") Set<Role> roles,
            @JsonProperty("ENABLED") boolean enabled,
            @JsonProperty("BOOKINGS") List<BookingResponseDto> bookings) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.enabled = enabled;
        this.bookings = bookings;
    }
}

