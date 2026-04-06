package com.worldcup.hotelbooking.user;

import com.worldcup.hotelbooking.booking.booking.BookingResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface AppUserService {

    AppUser createUser(AppUserRequestDto dto);

    AppUser getUserById(Long id);

    List<AppUser> getAllUsers();

    Optional<AppUser> getUserByEmail(String email);

    void deleteUser(Long id);

    AppUserResponseDto updateUser(Long id, AppUserRequestDto dto);

    List<BookingResponseDto> getUserBookings(Long userId);

    Page<AppUserResponseDto> getAllUsers(Pageable pageable);

    Page<AppUserResponseDto> getAllUsers(Pageable pageable, String username, String email);

    AppUser saveUser(AppUser user);

    List<AppUserResponseDto> searchUsers(String username, String email);
    AppUser partialUpdateUser(Long id, Map<String, Object> updates);

    AppUser updateUserRoles(Long id, Set<Role> roles);


}