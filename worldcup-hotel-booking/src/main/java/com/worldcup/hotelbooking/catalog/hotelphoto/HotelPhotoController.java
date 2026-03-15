package com.worldcup.hotelbooking.catalog.hotelphoto;

import com.worldcup.hotelbooking.catalog.hotelphoto.dto.HotelPhotoResponseDto;
import com.worldcup.hotelbooking.catalog.hotelphoto.dto.ReorderPhotosRequestDto;
import com.worldcup.hotelbooking.catalog.hotelphoto.mapper.HotelPhotoMapper;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/hotels/{hotelId}/photos")
public class HotelPhotoController {

    private final HotelPhotoService hotelPhotoService;
    private final HotelPhotoMapper hotelPhotoMapper;

    public HotelPhotoController(HotelPhotoService hotelPhotoService, HotelPhotoMapper hotelPhotoMapper) {
        this.hotelPhotoService = hotelPhotoService;
        this.hotelPhotoMapper = hotelPhotoMapper;
    }

    @GetMapping
    public List<HotelPhotoResponseDto> all(@PathVariable Long hotelId) {
        return hotelPhotoService.listPhotos(hotelId)
                .stream()
                .map(hotelPhotoMapper::toResponse)
                .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HotelPhotoResponseDto> upload(
            @PathVariable Long hotelId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
            UriComponentsBuilder uriBuilder
    ) {
        HotelPhoto saved = hotelPhotoService.addPhoto(hotelId, file, caption, sortOrder);

        URI location = uriBuilder.path("/hotels/{hotelId}/photos/{photoId}")
                .buildAndExpand(hotelId, saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(hotelPhotoMapper.toResponse(saved));
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> delete(@PathVariable Long hotelId, @PathVariable Long photoId) {
        hotelPhotoService.deletePhoto(hotelId, photoId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{photoId}/primary")
    public ResponseEntity<Void> setPrimary(
            @PathVariable Long hotelId,
            @PathVariable Long photoId
    ) {
        hotelPhotoService.setPrimaryPhoto(hotelId, photoId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorder(
            @PathVariable Long hotelId,
            @Valid @RequestBody ReorderPhotosRequestDto body
    ) {
        hotelPhotoService.reorderPhotos(hotelId, body.getPhotoIds());
        return ResponseEntity.noContent().build();
    }
}
