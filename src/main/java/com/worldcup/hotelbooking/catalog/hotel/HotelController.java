package com.worldcup.hotelbooking.catalog.hotel;

import com.worldcup.hotelbooking.catalog.hotel.dto.CreateHotelRequestDto;
import com.worldcup.hotelbooking.catalog.hotel.dto.HotelResponseDto;
import com.worldcup.hotelbooking.catalog.hotel.dto.ReplaceHotelRequestDto;
import com.worldcup.hotelbooking.catalog.hotel.dto.UpdateHotelPatchRequest;
import com.worldcup.hotelbooking.catalog.hotel.mapper.HotelMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/hotels")
@Tag(name = "Hotel Controller", description = "APIs for managing hotels")
public class HotelController {

    private final HotelService service;

    public HotelController(HotelService service) {
        this.service = service;
    }

    @Operation(
            summary = "Get all hotels",
            description = "Returns a list of all hotels in the system"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hotels retrieved successfully")
    })
    @GetMapping
    public List<HotelResponseDto> all() {
        return service.findAll().stream().map(HotelMapper::toResponse).toList();
    }

    @Operation(
            summary = "Create a hotel",
            description = "Creates a new hotel for the specified owner"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Hotel created successfully",
                    content = @Content(schema = @Schema(implementation = HotelResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HotelResponseDto> create(
            @Valid @RequestBody CreateHotelRequestDto body,
            Authentication authentication,
            UriComponentsBuilder uriBuilder
    ) {
        Long ownerId = extractUserId(authentication);

        Hotel entity = HotelMapper.fromCreate(body);
        Hotel saved = service.create(entity, ownerId);

        URI location = uriBuilder.path("/hotels/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(HotelMapper.toResponse(saved));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Unauthenticated user cannot create hotel");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            Object claim = jwt.getClaim("userId");

            if (claim instanceof Integer i) return i.longValue();
            if (claim instanceof Long l) return l;
            if (claim instanceof String s) {
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException ignored) {
                    throw new IllegalStateException("Invalid userId claim");
                }
            }
        }

        throw new IllegalStateException("userId claim is missing or invalid");
    }


    @Operation(
            summary = "Get hotel by id",
            description = "Returns a single hotel by its id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hotel retrieved successfully",
                    content = @Content(schema = @Schema(implementation = HotelResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content)
    })
    @GetMapping("/{id}")
    public HotelResponseDto one(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long id
    ) {
        return HotelMapper.toResponse(service.findById(id));
    }

    @Operation(
            summary = "Replace hotel",
            description = "Replaces all hotel data for the given hotel id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hotel replaced successfully",
                    content = @Content(schema = @Schema(implementation = HotelResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#id, authentication))")
    public ResponseEntity<HotelResponseDto> replace(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ReplaceHotelRequestDto body
    ) {
        Hotel incoming = HotelMapper.fromReplace(body);
        Hotel updated = service.replace(id, incoming);
        return ResponseEntity.ok(HotelMapper.toResponse(updated));
    }

    @Operation(
            summary = "Partially update hotel",
            description = "Updates only the provided fields of the hotel"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hotel updated successfully",
                    content = @Content(schema = @Schema(implementation = HotelResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content)
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#id, authentication))")
    public ResponseEntity<HotelResponseDto> patch(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateHotelPatchRequest body
    ) {
        Hotel updated = service.updatePartial(id, body);
        return ResponseEntity.ok(HotelMapper.toResponse(updated));
    }

    @Operation(
            summary = "Delete hotel",
            description = "Deletes a hotel by its id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Hotel deleted successfully", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#id, authentication))")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long id
    ) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get hotels by owner id",
            description = "Returns all hotels that belong to the given owner"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Owner hotels retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("hasRole('ADMIN') or @hotelAuthorizationService.canViewOwnerHotels(#ownerId, authentication)")
    public List<HotelResponseDto> getMyHotels(
            @Parameter(description = "Owner id", example = "1")
            @PathVariable Long ownerId
    ) {
        return service.getMyHotels(ownerId).stream().map(HotelMapper::toResponse).toList();
    }
}