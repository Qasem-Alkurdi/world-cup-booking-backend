package com.worldcup.hotelbooking.catalog.hotel;

import com.worldcup.hotelbooking.catalog.hotel.dto.CreateHotelRequestDto;
import com.worldcup.hotelbooking.catalog.hotel.dto.HotelResponseDto;
import com.worldcup.hotelbooking.catalog.hotel.dto.ReplaceHotelRequestDto;
import com.worldcup.hotelbooking.catalog.hotel.dto.UpdateHotelPatchRequest;
import com.worldcup.hotelbooking.catalog.hotel.mapper.HotelMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/hotels")
@Transactional
public class HotelController {

    private final HotelServiceInterface service;

    public HotelController(HotelServiceInterface service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<HotelResponseDto> all() {
        return service.findAll().stream().map(HotelMapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<HotelResponseDto> create(
            @Valid @RequestBody CreateHotelRequestDto body,
            UriComponentsBuilder uriBuilder
    ) {
        Hotel entity = HotelMapper.fromCreate(body);
        Hotel saved = service.create(entity, body.getOwnerId());

        URI location = uriBuilder.path("/hotels/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(HotelMapper.toResponse(saved));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public HotelResponseDto one(@PathVariable Long id) {
        return HotelMapper.toResponse(service.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HotelResponseDto> replace(
            @PathVariable Long id,
            @Valid @RequestBody ReplaceHotelRequestDto body
    ) {
        Hotel incoming = HotelMapper.fromReplace(body);
        Hotel updated = service.replace(id, incoming);
        return ResponseEntity.ok(HotelMapper.toResponse(updated));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<HotelResponseDto> patch(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHotelPatchRequest body
    ) {
        Hotel updated = service.updatePartial(id, body);
        return ResponseEntity.ok(HotelMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/owner/{ownerId}")
    @Transactional(readOnly = true)
    public List<HotelResponseDto> getMyHotels(@PathVariable Long ownerId) {
        return service.getMyHotels(ownerId).stream().map(HotelMapper::toResponse).toList();
    }
}
