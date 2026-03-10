package com.worldcup.hotelbooking.catalog.roomtype;

import com.worldcup.hotelbooking.catalog.roomtype.dto.CreateRoomTypeRequestDto;
import com.worldcup.hotelbooking.catalog.roomtype.dto.ReplaceRoomTypeRequestDto;
import com.worldcup.hotelbooking.catalog.roomtype.dto.RoomTypeResponseDto;
import com.worldcup.hotelbooking.catalog.roomtype.mapper.RoomTypeMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/hotels/{hotelId}/room-types")

public class RoomTypeController {

    private final RoomTypeService service;

    public RoomTypeController(RoomTypeService service) {
        this.service = service;
    }

    @GetMapping

    public List<RoomTypeResponseDto> all(@PathVariable Long hotelId) {
        return service.findByHotel(hotelId)
                .stream()
                .map(RoomTypeMapper::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<RoomTypeResponseDto> create(
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

    @GetMapping("/{id}")

    public RoomTypeResponseDto one(@PathVariable Long hotelId, @PathVariable Long id) {
        return RoomTypeMapper.toResponse(service.findById(hotelId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoomTypeResponseDto> replace(
            @PathVariable Long hotelId,
            @PathVariable Long id,
            @Valid @RequestBody ReplaceRoomTypeRequestDto body
    ) {
        RoomType updated = service.replace(hotelId, id, body);
        return ResponseEntity.ok(RoomTypeMapper.toResponse(updated));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long hotelId, @PathVariable Long id) {
        service.delete(hotelId, id);
        return ResponseEntity.noContent().build();
    }
}
