package com.worldcup.hotelbooking.user.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.worldcup.hotelbooking.booking.booking.BookingResponseDto;

import java.util.List;
import java.util.Set;


public class AppUserResponseDto {

    private final Long id;
    private final String username;
    private final String email;
    private final Set<Role> roles;
    private final boolean enabled;
    private final List<BookingResponseDto> bookings;

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

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<BookingResponseDto> getBookings() {
        return bookings;
    }
}

