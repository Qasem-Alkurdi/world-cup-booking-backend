package com.worldcup.hotelbooking.user.user;

import com.worldcup.hotelbooking.booking.booking.BookingResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;

public interface AppUserService {
    // Original methods
    AppUser createUser(AppUserRequestDto dto);

    AppUser getUserById(Long id);

    List<AppUser> getAllUsers();

    Optional<AppUser> getUserByEmail(String email);

    void deleteUser(Long id);

    AppUser updateUser(Long id, AppUserRequestDto dto);

    List<BookingResponseDto> getUserBookings(Long userId);

    // New methods
    Page<AppUser> getAllUsers(Pageable pageable);

    AppUser saveUser(AppUser user);

    // Add search method to interface
    List<AppUser> searchUsers(String username, String email);

    AppUser partialUpdateUser(Long id, Map<String, Object> updates);

    AppUser updateUserRoles(Long id, Set<Role> roles);
}