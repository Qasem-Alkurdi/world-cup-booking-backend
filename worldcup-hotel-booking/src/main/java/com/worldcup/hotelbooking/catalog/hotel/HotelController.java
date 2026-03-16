package com.worldcup.hotelbooking.catalog.hotel;

import com.worldcup.hotelbooking.catalog.hotel.dto.CreateHotelRequestDto;
import com.worldcup.hotelbooking.catalog.hotel.dto.HotelResponseDto;
import com.worldcup.hotelbooking.catalog.hotel.dto.ReplaceHotelRequestDto;
import com.worldcup.hotelbooking.catalog.hotel.dto.UpdateHotelPatchRequest;
import com.worldcup.hotelbooking.catalog.hotel.mapper.HotelMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/hotels")
public class HotelController {

    private final HotelService service;

    public HotelController(HotelService service) {
        this.service = service;
    }

    @GetMapping
    public List<HotelResponseDto> all() {
        return service.findAll().stream().map(HotelMapper::toResponse).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canCreateHotelForOwner(#body.ownerId, authentication))")
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
    public HotelResponseDto one(@PathVariable Long id) {
        return HotelMapper.toResponse(service.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#id, authentication))")
    public ResponseEntity<HotelResponseDto> replace(
            @PathVariable Long id,
            @Valid @RequestBody ReplaceHotelRequestDto body
    ) {
        Hotel incoming = HotelMapper.fromReplace(body);
        Hotel updated = service.replace(id, incoming);
        return ResponseEntity.ok(HotelMapper.toResponse(updated));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#id, authentication))")
    public ResponseEntity<HotelResponseDto> patch(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHotelPatchRequest body
    ) {
        Hotel updated = service.updatePartial(id, body);
        return ResponseEntity.ok(HotelMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @hotelAuthorizationService.canManageHotel(#id, authentication))")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("hasRole('ADMIN') or @hotelAuthorizationService.canViewOwnerHotels(#ownerId, authentication)")
    public List<HotelResponseDto> getMyHotels(@PathVariable Long ownerId) {
        return service.getMyHotels(ownerId).stream().map(HotelMapper::toResponse).toList();
    }
}