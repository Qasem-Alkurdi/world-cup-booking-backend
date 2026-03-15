package com.worldcup.hotelbooking.catalog.roomtypephoto;

import com.worldcup.hotelbooking.catalog.roomtypephoto.dto.ReorderPhotosRequestDto;
import com.worldcup.hotelbooking.catalog.roomtypephoto.dto.RoomTypePhotoResponseDto;
import com.worldcup.hotelbooking.catalog.roomtypephoto.mapper.RoomTypePhotoMapper;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/hotels/{hotelId}/room-types/{roomTypeId}/photos")
public class RoomTypePhotoController {

    private final RoomTypePhotoService roomTypePhotoService;
    private final RoomTypePhotoMapper roomTypePhotoMapper;

    public RoomTypePhotoController(RoomTypePhotoService roomTypePhotoService,
                                   RoomTypePhotoMapper roomTypePhotoMapper) {
        this.roomTypePhotoService = roomTypePhotoService;
        this.roomTypePhotoMapper = roomTypePhotoMapper;
    }

    @GetMapping
    public List<RoomTypePhotoResponseDto> all(@PathVariable Long hotelId, @PathVariable Long roomTypeId) {
        return roomTypePhotoService.listPhotos(hotelId, roomTypeId)
                .stream()
                .map(roomTypePhotoMapper::toResponse)
                .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RoomTypePhotoResponseDto> upload(
            @PathVariable Long hotelId,
            @PathVariable Long roomTypeId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
            UriComponentsBuilder uriBuilder
    ) {
        RoomTypePhoto saved = roomTypePhotoService.addPhoto(hotelId, roomTypeId, file, caption, sortOrder);

        URI location = uriBuilder
                .path("/hotels/{hotelId}/room-types/{roomTypeId}/photos/{photoId}")
                .buildAndExpand(hotelId, roomTypeId, saved.getId())
                .toUri();

        return ResponseEntity.created(location).body(roomTypePhotoMapper.toResponse(saved));
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long hotelId,
            @PathVariable Long roomTypeId,
            @PathVariable Long photoId
    ) {
        roomTypePhotoService.deletePhoto(hotelId, roomTypeId, photoId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{photoId}/primary")
    public ResponseEntity<Void> setPrimary(
            @PathVariable Long hotelId,
            @PathVariable Long roomTypeId,
            @PathVariable Long photoId
    ) {
        roomTypePhotoService.setPrimaryPhoto(hotelId, roomTypeId, photoId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorder(
            @PathVariable Long hotelId,
            @PathVariable Long roomTypeId,
            @Valid @RequestBody ReorderPhotosRequestDto body
    ) {
        roomTypePhotoService.reorderPhotos(hotelId, roomTypeId, body.getPhotoIds());
        return ResponseEntity.noContent().build();
    }
}
