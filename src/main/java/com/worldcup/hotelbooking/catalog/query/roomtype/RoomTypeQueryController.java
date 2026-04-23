package com.worldcup.hotelbooking.catalog.query.roomtype;

import com.worldcup.hotelbooking.catalog.query.roomtype.dto.RoomTypeQueryResponseDto;
import com.worldcup.hotelbooking.catalog.roomtype.dto.RoomTypeAvailabilityCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only endpoint for browsing room types within a hotel.
 *
 * <p>Separated from the write-side {@code RoomTypeController} following CQRS:
 * this controller <strong>only handles GET requests</strong> and returns a
 * richer DTO that includes resolved photo URLs and full amenity details,
 * avoiding any secondary round-trips from the client.
 *
 * <p>Endpoint: {@code GET /hotels/{hotelId}/room-types}
 * <br>Query params (all optional):
 * <ul>
 *   <li>{@code checkInDate} / {@code checkOutDate} — availability window</li>
 *   <li>{@code adults} / {@code children} — capacity requirements</li>
 *   <li>{@code numberOfRooms} — how many rooms the guest needs</li>
 * </ul>
 */
@RestController
@RequestMapping("/hotels/{hotelId}/room-types")
@Tag(name = "Room Type Query", description = "Read-only APIs for browsing hotel room types (CQRS read side)")
public class RoomTypeQueryController {

    private final RoomTypeQueryService roomTypeQueryService;

    public RoomTypeQueryController(RoomTypeQueryService roomTypeQueryService) {
        this.roomTypeQueryService = roomTypeQueryService;
    }

    @Operation(
            summary = "List available room types for a hotel",
            description = """
                    Returns all room types for the specified hotel that satisfy the given
                    availability and capacity criteria. Each room type includes:
                    - Full amenity details (AC, TV, Balcony, Safe, etc.)
                    - Resolved primary and gallery photo URLs
                    - Pricing in the hotel's configured currency
                    
                    Results are sorted by base price ascending.
                    No criteria → returns all room types for the hotel.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room types retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid availability criteria", content = @Content),
            @ApiResponse(responseCode = "404", description = "Hotel not found", content = @Content)
    })
    @GetMapping
    public List<RoomTypeQueryResponseDto> listAvailable(
            @Parameter(description = "Hotel ID", example = "1")
            @PathVariable Long hotelId,

            @Parameter(description = "Availability & capacity filter criteria")
            @ModelAttribute RoomTypeAvailabilityCriteria criteria
    ) {
        return roomTypeQueryService.findAvailableByHotel(hotelId, criteria);
    }
}
