package com.worldcup.hotelbooking.catalog.roomtypephoto;

import com.worldcup.hotelbooking.catalog.roomtypephoto.dto.ReorderPhotosRequestDto;
import com.worldcup.hotelbooking.catalog.roomtypephoto.dto.RoomTypePhotoResponseDto;
import com.worldcup.hotelbooking.catalog.roomtypephoto.mapper.RoomTypePhotoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/hotels/{hotelId}/room-types/{roomTypeId}/photos")
@Tag(name = "Room Type Photo Controller", description = "APIs for managing room type photos")
public class RoomTypePhotoController {

    private final RoomTypePhotoService roomTypePhotoService;
    private final RoomTypePhotoMapper roomTypePhotoMapper;

    public RoomTypePhotoController(RoomTypePhotoService roomTypePhotoService,
                                   RoomTypePhotoMapper roomTypePhotoMapper) {
        this.roomTypePhotoService = roomTypePhotoService;
        this.roomTypePhotoMapper = roomTypePhotoMapper;
    }

    @Operation(
            summary = "Get all room type photos",
            description = "Returns all photos for the specified room type"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room type photos retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Hotel or room type not found", content = @Content)
    })
    @GetMapping
    public List<RoomTypePhotoResponseDto> all(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "Room type id", example = "5")
            @PathVariable Long roomTypeId
    ) {
        return roomTypePhotoService.listPhotos(hotelId, roomTypeId)
                .stream()
                .map(roomTypePhotoMapper::toResponse)
                .toList();
    }

    @Operation(
            summary = "Upload room type photo",
            description = "Uploads a new photo for the specified room type"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Room type photo uploaded successfully",
                    content = @Content(schema = @Schema(implementation = RoomTypePhotoResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid multipart request", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel or room type not found", content = @Content)
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#hotelId, authentication))")
    public ResponseEntity<RoomTypePhotoResponseDto> upload(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "Room type id", example = "5")
            @PathVariable Long roomTypeId,
            @Parameter(description = "Photo file to upload")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Photo caption", example = "Deluxe room view")
            @RequestPart(value = "caption", required = false) String caption,
            @Parameter(description = "Display order of the photo", example = "1")
            @RequestPart(value = "sortOrder", required = false) Integer sortOrder,
            UriComponentsBuilder uriBuilder
    ) {
        RoomTypePhoto saved = roomTypePhotoService.addPhoto(hotelId, roomTypeId, file, caption, sortOrder);

        URI location = uriBuilder
                .path("/hotels/{hotelId}/room-types/{roomTypeId}/photos/{photoId}")
                .buildAndExpand(hotelId, roomTypeId, saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(roomTypePhotoMapper.toResponse(saved));
    }

    @Operation(
            summary = "Delete room type photo",
            description = "Deletes a specific photo from the specified room type"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Room type photo deleted successfully", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel, room type, or photo not found", content = @Content)
    })
    @DeleteMapping("/{photoId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#hotelId, authentication))")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "Room type id", example = "5")
            @PathVariable Long roomTypeId,
            @Parameter(description = "Photo id", example = "10")
            @PathVariable Long photoId
    ) {
        roomTypePhotoService.deletePhoto(hotelId, roomTypeId, photoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Set primary room type photo",
            description = "Marks a specific room type photo as the primary photo"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Primary room type photo set successfully", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel, room type, or photo not found", content = @Content)
    })
    @PatchMapping("/{photoId}/primary")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#hotelId, authentication))")
    public ResponseEntity<Void> setPrimary(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "Room type id", example = "5")
            @PathVariable Long roomTypeId,
            @Parameter(description = "Photo id", example = "10")
            @PathVariable Long photoId
    ) {
        roomTypePhotoService.setPrimaryPhoto(hotelId, roomTypeId, photoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reorder room type photos",
            description = "Reorders room type photos based on the provided list of photo ids"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Room type photos reordered successfully", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid reorder request", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel, room type, or one of the photos not found", content = @Content)
    })
    @PatchMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#hotelId, authentication))")
    public ResponseEntity<Void> reorder(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "Room type id", example = "5")
            @PathVariable Long roomTypeId,
            @RequestBody ReorderPhotosRequestDto body
    ) {
        roomTypePhotoService.reorderPhotos(hotelId, roomTypeId, body.getPhotoIds());
        return ResponseEntity.noContent().build();
    }
}