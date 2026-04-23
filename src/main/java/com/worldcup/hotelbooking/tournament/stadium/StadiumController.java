package com.worldcup.hotelbooking.tournament.stadium;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stadiums")
@RequiredArgsConstructor
@Tag(name = "Stadiums", description = "Public stadium information and admin management")
public class StadiumController {

    private final StadiumService stadiumService;

    @GetMapping
    @Operation(summary = "Get all stadiums with pagination")
    public ResponseEntity<Page<Stadium>> getAllStadiums(
            @RequestParam(required = false) String city,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Page<Stadium> page = stadiumService.getAllStadiums(city, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stadium by ID")
    public ResponseEntity<Stadium> getStadiumById(@PathVariable Long id) {
        return ResponseEntity.ok(stadiumService.getStadiumById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new stadium (Admin only)")
    public ResponseEntity<Stadium> createStadium(@Valid @RequestBody Stadium stadium) {
        Stadium created = stadiumService.createStadium(stadium);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing stadium (Admin only)")
    public ResponseEntity<Stadium> updateStadium(@PathVariable Long id, @Valid @RequestBody Stadium stadium) {
        Stadium updated = stadiumService.updateStadium(id, stadium);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a stadium (Admin only)")
    public ResponseEntity<Void> deleteStadium(@PathVariable Long id) {
        stadiumService.deleteStadium(id);
        return ResponseEntity.noContent().build();
    }
}