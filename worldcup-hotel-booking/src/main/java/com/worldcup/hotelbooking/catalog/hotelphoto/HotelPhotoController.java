package com.worldcup.hotelbooking.catalog.hotelphoto;

import com.worldcup.hotelbooking.catalog.hotelphoto.dto.HotelPhotoResponseDto;
import com.worldcup.hotelbooking.catalog.hotelphoto.dto.ReorderPhotosRequestDto;
import com.worldcup.hotelbooking.catalog.hotelphoto.mapper.HotelPhotoMapper;
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
@RequestMapping("/hotels/{hotelId}/photos")
@Tag(name = "Hotel Photo Controller", description = "APIs for managing hotel photos")
public class HotelPhotoController {

    private final HotelPhotoService hotelPhotoService;
    private final HotelPhotoMapper hotelPhotoMapper;

    public HotelPhotoController(HotelPhotoService hotelPhotoService, HotelPhotoMapper hotelPhotoMapper) {
        this.hotelPhotoService = hotelPhotoService;
        this.hotelPhotoMapper = hotelPhotoMapper;
    }

    @Operation(
            summary = "Get all hotel photos",
            description = "Returns all photos for the specified hotel"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hotel photos retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content)
    })
    @GetMapping
    public List<HotelPhotoResponseDto> all(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId
    ) {
        return hotelPhotoService.listPhotos(hotelId)
                .stream()
                .map(hotelPhotoMapper::toResponse)
                .toList();
    }

    @Operation(
            summary = "Upload hotel photo",
            description = "Uploads a new photo for the specified hotel"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Hotel photo uploaded successfully",
                    content = @Content(schema = @Schema(implementation = HotelPhotoResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid multipart request", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content)
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#hotelId, authentication))")
    public ResponseEntity<HotelPhotoResponseDto> upload(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "Photo file to upload")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Photo caption", example = "Main hotel entrance")
            @RequestPart(value = "caption", required = false) String caption,
            @Parameter(description = "Display order of the photo", example = "1")
            @RequestPart(value = "sortOrder", required = false) Integer sortOrder,
            UriComponentsBuilder uriBuilder
    ) {
        HotelPhoto saved = hotelPhotoService.addPhoto(hotelId, file, caption, sortOrder);

        URI location = uriBuilder.path("/hotels/{hotelId}/photos/{photoId}")
                .buildAndExpand(hotelId, saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(hotelPhotoMapper.toResponse(saved));
    }

    @Operation(
            summary = "Delete hotel photo",
            description = "Deletes a specific photo from the specified hotel"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Hotel photo deleted successfully", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel or photo not found", content = @Content)
    })
    @DeleteMapping("/{photoId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#hotelId, authentication))")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "Photo id", example = "10")
            @PathVariable Long photoId
    ) {
        hotelPhotoService.deletePhoto(hotelId, photoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Set primary hotel photo",
            description = "Marks a specific hotel photo as the primary photo"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Primary hotel photo set successfully", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel or photo not found", content = @Content)
    })
    @PatchMapping("/{photoId}/primary")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#hotelId, authentication))")
    public ResponseEntity<Void> setPrimary(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @Parameter(description = "Photo id", example = "10")
            @PathVariable Long photoId
    ) {
        hotelPhotoService.setPrimaryPhoto(hotelId, photoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Reorder hotel photos",
            description = "Reorders hotel photos based on the provided list of photo ids"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Hotel photos reordered successfully", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid reorder request", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel or one of the photos not found", content = @Content)
    })
    @PatchMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#hotelId, authentication))")
    public ResponseEntity<Void> reorder(
            @Parameter(description = "Hotel id", example = "1")
            @PathVariable Long hotelId,
            @RequestBody ReorderPhotosRequestDto body
    ) {
        hotelPhotoService.reorderPhotos(hotelId, body.getPhotoIds());
        return ResponseEntity.noContent().build();
    }
}