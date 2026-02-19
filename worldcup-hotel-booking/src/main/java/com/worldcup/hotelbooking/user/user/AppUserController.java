package com.worldcup.hotelbooking.user.user;

import com.worldcup.hotelbooking.booking.booking.BookingResponseDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    // Create user
    @PostMapping
    public ResponseEntity<AppUserResponseDto> createUser(
            @Valid @RequestBody AppUserRequestDto dto,
            UriComponentsBuilder uriBuilder) {

        AppUser createdUser = appUserService.createUser(dto);
        AppUserResponseDto responseDto = AppUserMapper.toDto(createdUser);

        return ResponseEntity
                .created(uriBuilder.path("/users/{id}").buildAndExpand(createdUser.getId()).toUri())
                .body(responseDto);
    }

    // Get user by id
    @GetMapping("/{id}")
    public ResponseEntity<AppUserResponseDto> getUserById(@PathVariable Long id) {
        AppUser user = appUserService.getUserById(id);
        AppUserResponseDto responseDto = AppUserMapper.toDto(user);
        return ResponseEntity.ok(responseDto);
    }

    // Get all users with pagination
    @GetMapping
    public ResponseEntity<Page<AppUserResponseDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<AppUser> usersPage = appUserService.getAllUsers(pageable);   // ✅ use service
        Page<AppUserResponseDto> responsePage = usersPage.map(AppUserMapper::toDto);

        return ResponseEntity.ok(responsePage);
    }

    // Get user by email
    @GetMapping("/email/{email}")
    public ResponseEntity<AppUserResponseDto> getUserByEmail(@PathVariable String email) {
        return appUserService.getUserByEmail(email)
                .map(AppUserMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update user (full update - replaces entire resource)
    @PutMapping("/{id}")
    public ResponseEntity<AppUserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AppUserRequestDto dto) {

        AppUser updatedUser = appUserService.updateUser(id, dto);
        AppUserResponseDto responseDto = AppUserMapper.toDto(updatedUser);
        return ResponseEntity.ok(responseDto);
    }

    // Partial update user (update only provided fields)
    @PatchMapping("/{id}")
    public ResponseEntity<AppUserResponseDto> partialUpdateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        AppUser updatedUser = appUserService.partialUpdateUser(id, updates);
        AppUserResponseDto responseDto = AppUserMapper.toDto(updatedUser);
        return ResponseEntity.ok(responseDto);
    }

    // Delete user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        appUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Get user bookings
    @GetMapping("/{id}/bookings")
    public ResponseEntity<List<BookingResponseDto>> getUserBookings(@PathVariable Long id) {
        List<BookingResponseDto> bookings = appUserService.getUserBookings(id);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/search")
    public ResponseEntity<List<AppUserResponseDto>> searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {

        // Use the service method (which uses repository for database-level search)
        List<AppUser> users = appUserService.searchUsers(username, email);

        // Convert to DTOs
        List<AppUserResponseDto> responseDto = users.stream()
                .map(AppUserMapper::toDto)
                .toList();

        return ResponseEntity.ok(responseDto);
    }
}