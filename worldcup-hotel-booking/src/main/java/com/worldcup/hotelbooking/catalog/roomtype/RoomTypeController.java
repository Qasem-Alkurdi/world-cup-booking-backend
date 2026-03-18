package com.worldcup.hotelbooking.catalog.roomtype;

import com.worldcup.hotelbooking.catalog.roomtype.dto.CreateRoomTypeRequestDto;
import com.worldcup.hotelbooking.catalog.roomtype.dto.ReplaceRoomTypeRequestDto;
import com.worldcup.hotelbooking.catalog.roomtype.dto.RoomTypeAvailabilityCriteria;
import com.worldcup.hotelbooking.catalog.roomtype.dto.RoomTypeResponseDto;
import com.worldcup.hotelbooking.catalog.roomtype.mapper.RoomTypeMapper;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/hotels/{hotelId}/room-types")
@Tag(name = "Room Type Controller", description = "APIs for managing hotel room types")
public class RoomTypeController {

    private final RoomTypeService service;

    public RoomTypeController(RoomTypeService service) {
        this.service = service;
    }

    @Operation(
            summary = "Get room types by hotel",
            description = "Returns room types for the specified hotel, optionally filtered by availability criteria"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room types retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content)
    })
    @GetMapping
    public List<RoomTypeResponseDto> all(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "Availability filter criteria")
            @ModelAttribute RoomTypeAvailabilityCriteria criteria
    ) {
        return service.findAvailableByHotel(hotelId, criteria)
                .stream()
                .map(RoomTypeMapper::toResponse)
                .toList();
    }

    @Operation(
            summary = "Create room type",
            description = "Creates a new room type for the specified hotel"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Room type created successfully",
                    content = @Content(schema = @Schema(implementation = RoomTypeResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#hotelId, authentication))")
    public ResponseEntity<RoomTypeResponseDto> create(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @Valid @RequestBody CreateRoomTypeRequestDto body,
            UriComponentsBuilder uriBuilder
    ) {
        RoomType entity = RoomTypeMapper.fromCreate(body);
        RoomType saved = service.create(hotelId, entity);

        URI location = uriBuilder
                .path("/hotels/{hotelId}/room-types/{id}")
                .buildAndExpand(hotelId, saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(RoomTypeMapper.toResponse(saved));
    }

    @Operation(
            summary = "Get room type by id",
            description = "Returns a specific room type for the specified hotel"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room type retrieved successfully",
                    content = @Content(schema = @Schema(implementation = RoomTypeResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Hotel or room type not found", content = @Content)
    })
    @GetMapping("/{id}")
    public RoomTypeResponseDto one(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "Room type id", example = "5")
            @PathVariable Long id
    ) {
        return RoomTypeMapper.toResponse(service.findById(hotelId, id));
    }

    @Operation(
            summary = "Replace room type",
            description = "Replaces all room type data for the specified hotel and room type id"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room type replaced successfully",
                    content = @Content(schema = @Schema(implementation = RoomTypeResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel or room type not found", content = @Content)
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#hotelId, authentication))")
    public ResponseEntity<RoomTypeResponseDto> replace(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "Room type id", example = "5")
            @PathVariable Long id,
            @Valid @RequestBody ReplaceRoomTypeRequestDto body
    ) {
        RoomType updated = service.replace(hotelId, id, body);
        return ResponseEntity.ok(RoomTypeMapper.toResponse(updated));
    }

    @Operation(
            summary = "Delete room type",
            description = "Deletes a room type from the specified hotel"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Room type deleted successfully", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel or room type not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#hotelId, authentication))")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "Room type id", example = "5")
            @PathVariable Long id
    ) {
        service.delete(hotelId, id);
        return ResponseEntity.noContent().build();
    }
}