package com.worldcup.hotelbooking.user;

import com.worldcup.hotelbooking.reservation.booking.BookingResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User management and profile operations")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.claims['userId']")
    @Operation(summary = "Get user by ID (user themselves or admin)")
    public ResponseEntity<AppUserResponseDto> getUserById(@PathVariable Long id) {
        AppUser user = appUserService.getUserById(id);
        return ResponseEntity.ok(AppUserMapper.toDto(user));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users with pagination (Admin only)")
    public ResponseEntity<Page<AppUserResponseDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Role role) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<AppUserResponseDto> responsePage = appUserService.getAllUsers(pageable, username, email, role);
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by email (Admin only)")
    public ResponseEntity<AppUserResponseDto> getUserByEmail(@PathVariable String email) {
        return appUserService.getUserByEmail(email)
                .map(AppUserMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new AppUserNotFoundException("User not found with email: " + email));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.claims['userId']")
    @Operation(summary = "Update user fully (user themselves or admin)")
    public ResponseEntity<AppUserResponseDto> updateUser(@PathVariable Long id, @Valid @RequestBody AppUserRequestDto dto) {
        AppUserResponseDto updated = appUserService.updateUser(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.claims['userId']")
    @Operation(summary = "Partial update of user fields (user themselves or admin)")
    public ResponseEntity<AppUserResponseDto> partialUpdateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        AppUser updatedUser = appUserService.partialUpdateUser(id, updates);
        return ResponseEntity.ok(AppUserMapper.toDto(updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.claims['userId']")
    @Operation(summary = "Delete user (user themselves or admin)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        appUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/bookings")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.claims['userId']")
    @Operation(summary = "Get all bookings of a user (user themselves or admin)")
    public ResponseEntity<List<BookingResponseDto>> getUserBookings(@PathVariable Long id) {
        List<BookingResponseDto> bookings = appUserService.getUserBookings(id);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username or email (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppUserResponseDto>> searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {
        List<AppUserResponseDto> responseDto = appUserService.searchUsers(username, email);
        return ResponseEntity.ok(responseDto);
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user roles (Admin only)")
    public ResponseEntity<AppUserResponseDto> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateDto dto) {

        AppUser updated = appUserService.updateUserRoles(id, dto.roles());
        return ResponseEntity.ok(AppUserMapper.toDto(updated));
    }

    @PostMapping("/{id}/profile-picture")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal.claims['userId'].toString()")
    @Operation(summary = "Upload profile picture")
    public ResponseEntity<AppUserResponseDto> uploadProfilePicture(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        AppUser updatedUser = appUserService.uploadProfilePicture(id, file);
        return ResponseEntity.ok(AppUserMapper.toDto(updatedUser));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user statistics (Admin only)")
    public ResponseEntity<Map<String, Long>> getUserStats() {
        return ResponseEntity.ok(appUserService.getUserStats());
    }
}