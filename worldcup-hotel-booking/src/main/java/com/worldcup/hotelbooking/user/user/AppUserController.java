package com.worldcup.hotelbooking.user.user;

import com.worldcup.hotelbooking.booking.booking.BookingResponseDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // Public: user registration
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

    // User themselves or admin
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<AppUserResponseDto> getUserById(@PathVariable Long id) {
        AppUser user = appUserService.getUserById(id);
        return ResponseEntity.ok(AppUserMapper.toDto(user));
    }

    // Admin only
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AppUserResponseDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<AppUser> usersPage = appUserService.getAllUsers(pageable);
        Page<AppUserResponseDto> responsePage = usersPage.map(AppUserMapper::toDto);
        return ResponseEntity.ok(responsePage);
    }

    // Admin only (or could be user themselves with email check, but simpler to keep admin)
    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppUserResponseDto> getUserByEmail(@PathVariable String email) {
        return appUserService.getUserByEmail(email)
                .map(AppUserMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // User themselves or admin
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<AppUserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AppUserRequestDto dto) {

        AppUser updatedUser = appUserService.updateUser(id, dto);
        return ResponseEntity.ok(AppUserMapper.toDto(updatedUser));
    }

    // User themselves or admin
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<AppUserResponseDto> partialUpdateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        AppUser updatedUser = appUserService.partialUpdateUser(id, updates);
        return ResponseEntity.ok(AppUserMapper.toDto(updatedUser));
    }

    // User themselves or admin
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        appUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // User themselves or admin
    @GetMapping("/{id}/bookings")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<List<BookingResponseDto>> getUserBookings(@PathVariable Long id) {
        List<BookingResponseDto> bookings = appUserService.getUserBookings(id);
        return ResponseEntity.ok(bookings);
    }

    // Admin only (search users)
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppUserResponseDto>> searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {

        List<AppUser> users = appUserService.searchUsers(username, email);
        List<AppUserResponseDto> responseDto = users.stream()
                .map(AppUserMapper::toDto)
                .toList();
        return ResponseEntity.ok(responseDto);
    }

    // Admin only – update user roles
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppUserResponseDto> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleUpdateDto dto) {

        AppUser updated = appUserService.updateUserRoles(id, dto.roles());
        return ResponseEntity.ok(AppUserMapper.toDto(updated));
    }
}