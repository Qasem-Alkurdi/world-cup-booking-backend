package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomMapper;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRequestDto;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomResponseDto;
import com.worldcup.hotelbooking.booking.cancellation.CancellationMapper;
import com.worldcup.hotelbooking.booking.cancellation.CancellationPolicyResponseDto;
import com.worldcup.hotelbooking.booking.cancellation.CancellationResponse;
import com.worldcup.hotelbooking.catalog.hotel.HotelService;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeService;
import com.worldcup.hotelbooking.common.response.PagedResponse;
import com.worldcup.hotelbooking.user.user.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);
    private final BookingServiceImpl bookingService;
    private final AppUserService appUserService;
    private final HotelService hotelService;
    private final RoomTypeService roomTypeService;

    BookingController(BookingServiceImpl bookingService, AppUserService appUserService, HotelService hotelService, RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
        this.hotelService = hotelService;
        this.appUserService = appUserService;
        this.bookingService = bookingService;
    }

    //get

    @Operation(summary = "Get booking by ID", description = "Retrieve a booking by its unique ID.")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @bookingAuthorizationService.isHimTheBookingUser(#id, authentication) or(hasRole('MANAGER') and @bookingAuthorizationService.isHimTheHotelOwnerOfTheBooking(#id, authentication))")
    public BookingResponseDto getBookingById(@PathVariable Long id) {
        return BookingMapper.toDto(bookingService.getBookingById(id));
    }

    @Operation(summary = "Create a new booking", description = "Create a new booking with the provided details. The request must include user ID, hotel ID, check-in and check-out dates, and room details.")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','GUEST')")
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody BookingRequestDto bookingRequest,
            UriComponentsBuilder uriBuilder) {

        // Create the booking entity
        Booking booking = BookingMapper.toEntity(
                bookingRequest,
                appUserService.getUserById(bookingRequest.getUserId()),
                hotelService.findById(bookingRequest.getHotelId())
        );

        // Create MUTABLE list for booking rooms
        List<BookingRoom> bookingRooms = new ArrayList<>();

        // Convert room requests to entities
        for (BookingRoomRequestDto roomRequest : bookingRequest.getRooms()) {
            BookingRoom bookingRoom = BookingRoomMapper.toEntity(
                    roomRequest,
                    booking,
                    roomTypeService.findById(bookingRequest.getHotelId(), roomRequest.getRoomTypeId())
            );
            bookingRooms.add(bookingRoom);
        }

        // Set the mutable list on the booking
        booking.setBookingRooms(bookingRooms);

        // Create the booking (this will save everything in one transaction)
        Booking createdBooking = bookingService.createBooking(booking);

        // Build response DTO
        BookingResponseDto responseDto = BookingMapper.toDto(createdBooking);

//        List<BookingRoomResponseDto> bookingRoomsResponseDto = new ArrayList<>();
//        // ✅ Populate the rooms list in the response
//        for (BookingRoom bookingRoom : createdBooking.getBookingRooms()) {
//            bookingRoomsResponseDto.add(BookingRoomMapper.toDto(bookingRoom));
//        }
//
//        responseDto.setRooms(bookingRoomsResponseDto);
        return ResponseEntity.created(
                uriBuilder.path("/bookings/{id}")
                        .buildAndExpand(createdBooking.getId())
                        .toUri()
        ).body(responseDto);
    }

    /**
     * Preview cancellation policy
     * Shows user what refund they would get WITHOUT actually cancelling
     * <p>
     * Example: GET /api/v1/bookings/123/cancellation-policy
     */
    @GetMapping("/{id}/cancellation-policy")
    @Operation(summary = "Preview cancellation policy", description = "Preview the cancellation policy for a specific booking. This endpoint allows users to see the refund amount and policy details without actually cancelling the booking.")
    @PreAuthorize("hasRole('ADMIN') or @bookingAuthorizationService.isHimTheBookingUser(#id, authentication) or(hasRole('MANAGER') and @bookingAuthorizationService.isHimTheHotelOwnerOfTheBooking(#id, authentication))")
    public ResponseEntity<CancellationPolicyResponseDto> getCancellationPolicy(@PathVariable Long id) {
        logger.info("GET request for cancellation policy for booking: {}", id);

        CancellationResponse result = (bookingService).previewCancellation(id);

        return ResponseEntity.ok(CancellationMapper.toDto(result));
    }

    /**
     * Updated cancel endpoint - now with policy enforcement
     * <p>
     * Example: PUT /api/v1/bookings/123/cancel?reason=Travel+plans+changed
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or @bookingAuthorizationService.isHimTheBookingUser(#id, authentication)")
    @Operation(summary = "Cancel a booking", description = "Cancel a specific booking by its ID. The cancellation reason must be provided as a query parameter. This endpoint will enforce the cancellation policy and return the refund amount and policy details in the response.")
    public ResponseEntity<BookingCancellationResponse> cancelBooking(
            @PathVariable Long id,
            @RequestParam String reason) {
        logger.info("PUT request to cancel booking {} with reason: {}", id, reason);

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }

        // Preview policy first to include in response
        CancellationResponse policyResult = (bookingService).previewCancellation(id);

        // Cancel the booking (will throw exception if not allowed)
        Booking cancelledBooking = bookingService.cancelBooking(id, reason);

        return ResponseEntity.ok(BookingMapper.toCancellationDto(cancelledBooking, policyResult));
    }


    @Operation(summary = "Get booking rooms", description = "Retrieve the list of rooms associated with a specific booking by its ID.")
    @GetMapping("/{id}/rooms")
    @PreAuthorize("hasRole('ADMIN') or @bookingAuthorizationService.isHimTheBookingUser(#id, authentication) or(hasRole('MANAGER') and @bookingAuthorizationService.isHimTheHotelOwnerOfTheBooking(#id, authentication))")
    public List<BookingRoomResponseDto> getBookingRooms(@PathVariable Long id) {
        return bookingService.getBookingById(id).getBookingRooms().stream().map(BookingRoomMapper::toDto).collect(toList());
    }

    @Operation(summary = "Get booking by reference", description = "Retrieve a booking using its unique booking reference code. This allows users to find their booking without needing the booking ID.")
    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasRole('ADMIN') or @bookingAuthorizationService.isHimTheBookingUser(#reference, authentication) or(hasRole('MANAGER') and @bookingAuthorizationService.isHimTheHotelOwnerOfTheBooking(#reference, authentication))")
    public BookingResponseDto getBookingByReference(@PathVariable String reference) {
        return BookingMapper.toDto(bookingService.findBookingByReference(reference));
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @bookingAuthorizationService.isHimTheBookingUser(#id, authentication)")
    public ResponseEntity<BookingResponseDto> updateBooking(
            @PathVariable long id,
            @RequestBody @Valid BookingRequestDto bookingRequest) {

        Booking booking = BookingMapper.toEntity(
                bookingRequest,
                appUserService.getUserById(bookingRequest.getUserId()),
                hotelService.findById(bookingRequest.getHotelId())
        );

        // ✅ CORRECT - Add to the booking's list
        for (BookingRoomRequestDto roomRequest : bookingRequest.getRooms()) {
            BookingRoom bookingRoom = BookingRoomMapper.toEntity(
                    roomRequest,
                    booking,
                    roomTypeService.findById(bookingRequest.getHotelId(), roomRequest.getRoomTypeId())
            );

            bookingRoom.setBooking(booking);
            booking.getBookingRooms().add(bookingRoom);
        }

        Booking updated = bookingService.updateExisting(id, booking);
        BookingResponseDto responseDto = BookingMapper.toDto(updated);

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/my/history")
    @Operation(summary = "Get my booking history", description = "Retrieve the booking history for the currently authenticated user. This endpoint returns a paginated list of past bookings made by the user.")
    @PreAuthorize("hasRole('ADMIN') or @bookingAuthorizationService.isCurrentUser(#userId, authentication)")
    public ResponseEntity<PagedResponse<BookingResponseDto>> getMyHistory(
            @RequestParam Long userId,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        Page<Booking> page = bookingService.getGuestHistory(userId, pageable);

        List<BookingResponseDto> content = page.getContent()
                .stream()
                .map(BookingMapper::toDto)
                .toList();

        return ResponseEntity.ok(
                PagedResponse.from(page, content)
        );
    }

    @GetMapping("/hotel/{hotelId}/upcoming")
    @Operation(summary = "Get upcoming bookings for a hotel", description = "Retrieve a paginated list of upcoming bookings for a specific hotel. This endpoint is intended for hotel managers to view future reservations.")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @bookingAuthorizationService.isHimTheHotelOwnerOfTheBookings(#hotelId, authentication))")
    public ResponseEntity<PagedResponse<BookingResponseDto>> getUpcoming(
            @PathVariable Long hotelId,
            @PageableDefault(size = 10, sort = "checkInDate")
            Pageable pageable
    ) {

        Page<Booking> page = bookingService.getHotelUpcomingBookings(hotelId, pageable);

        List<BookingResponseDto> content = page.getContent()
                .stream()
                .map(BookingMapper::toDto)
                .toList();

        return ResponseEntity.ok(
                PagedResponse.from(page, content)
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @bookingAuthorizationService.isHimTheHotelOwnerOfTheBookings(#hotelId, authentication))")
    public PagedResponse<BookingResponseDto> filterBookings(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) Booking.BookingStatus status,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @PageableDefault(size = 10, sort = "checkInDate")
            Pageable pageable
    ) {
        Page<Booking> page = bookingService.filterBookings(userId,
                hotelId,
                status,
                fromDate,
                toDate,
                minPrice,
                maxPrice,
                pageable);

        List<BookingResponseDto> content = page.getContent()
                .stream()
                .map(BookingMapper::toDto)
                .toList();

        return PagedResponse.from(page, content);

    }



    @PutMapping("/{id}/checkin")
    @Operation(summary = "Check-in a booking", description = "Mark a specific booking as checked in. This endpoint is intended for hotel staff to update the status of a booking when the guest arrives.")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @bookingAuthorizationService.isHimTheHotelOwnerOfTheBooking(#id, authentication))")
    public ResponseEntity<BookingResponseDto> checkInBooking(@PathVariable Long id) {
        Booking checkedIn = bookingService.checkInBooking(id);
        return ResponseEntity.ok(BookingMapper.toDto(checkedIn));
    }

    @PutMapping("/{id}/checkout")
    @Operation(summary = "Check-out a booking", description = "Mark a specific booking as checked out. This endpoint is intended for hotel staff to update the status of a booking when the guest departs.")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @bookingAuthorizationService.isHimTheHotelOwnerOfTheBooking(#id, authentication))")
    public ResponseEntity<BookingResponseDto> checkOutBooking(@PathVariable Long id) {
        Booking checkedOut = bookingService.checkOutBooking(id);
        return ResponseEntity.ok(BookingMapper.toDto(checkedOut));
    }

    /**
     * Hotel manager cancels a guest booking with automatic compensation bonus.
     *
     * The guest always receives a full 100% base refund PLUS a bonus that increases
     * the closer to check-in the cancellation happens:
     *
     *   30+ days  → +10%    14-29 days → +25%    7-13 days → +35%
     *    3-6 days  → +40%    < 3 days   → +50%
     *
     * Example: $200 booking cancelled 2 days before check-in
     *   base refund $200 + 50% bonus $100 = $300 total payout
     *
     * PUT /bookings/123/manager-cancel?reason=Hotel+renovation
     */
    @PutMapping("/{id}/manager-cancel")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @bookingAuthorizationService.isHimTheHotelOwnerOfTheBooking(#id, authentication))")
    @Operation(
            summary = "Manager cancels a guest booking",
            description = """
                    Cancels a booking on behalf of the hotel. The guest receives:
                    - A full 100% base refund, PLUS
                    - A compensation bonus (10% up to 50%) that increases the closer to check-in.
                    The manager's identity is read server-side from the Security context.
                    """)
    public ResponseEntity<BookingCancellationResponse> cancelBookingByManager(
            @PathVariable Long id,
            @RequestParam String reason) {

        logger.info("Manager '{}' requesting cancellation of booking id={}", id);

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }


        // Preview to get bonus breakdown for the response
        CancellationResponse policyResult =
                bookingService.previewManagerCancellation(id);

        // Perform the actual cancellation
        Booking cancelledBooking = bookingService.cancelBookingByManager(id, reason,"Hotel manger");

        return ResponseEntity.ok(BookingMapper.toManagerCancellationDto(cancelledBooking, policyResult));
    }

    @GetMapping("/{id}/manager-cancellation-preview")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @bookingAuthorizationService.isHimTheHotelOwnerOfTheBooking(#id, authentication))")
    @Operation(
            summary = "Preview manager cancellation policy",
            description = """
                    Preview the cancellation policy and compensation bonus if the hotel were to cancel this booking.
                    Shows the guest's base refund (always 100%) plus the bonus amount and tier that would apply based on how close to check-in the cancellation occurs.
                    """)
    public ResponseEntity<CancellationPolicyResponseDto> previewManagerCancellation(
            @PathVariable Long id) {


        CancellationResponse policyResult =
                bookingService.previewManagerCancellation(id);

        // We can return the same DTO as the actual cancellation since it includes all the necessary info
        return ResponseEntity.ok(CancellationMapper.toDto(policyResult));
    }
}