package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomMapper;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRequestDto;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomResponseDto;
import com.worldcup.hotelbooking.booking.cancellation.CancellationMapper;
import com.worldcup.hotelbooking.booking.cancellation.CancellationPolicyResponseDto;
import com.worldcup.hotelbooking.booking.cancellation.CancellationResponseDto;
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
    public BookingResponseDto getBookingById(@PathVariable Long id) {
        return BookingMapper.toDto(bookingService.getBookingById(id));
    }

    @Operation(summary = "Create a new booking", description = "Create a new booking with the provided details. The request must include user ID, hotel ID, check-in and check-out dates, and room details.")
    @PostMapping
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
    public ResponseEntity<CancellationPolicyResponseDto> getCancellationPolicy(@PathVariable Long id) {
        logger.info("GET request for cancellation policy for booking: {}", id);

        CancellationResponseDto result = (bookingService).previewCancellation(id);

        return ResponseEntity.ok(CancellationMapper.toDto(result));
    }

    /**
     * Updated cancel endpoint - now with policy enforcement
     * <p>
     * Example: PUT /api/v1/bookings/123/cancel?reason=Travel+plans+changed
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingCancellationResponse> cancelBooking(
            @PathVariable Long id,
            @RequestParam String reason) {
        logger.info("PUT request to cancel booking {} with reason: {}", id, reason);

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }

        // Preview policy first to include in response
        CancellationResponseDto policyResult = (bookingService).previewCancellation(id);

        // Cancel the booking (will throw exception if not allowed)
        Booking cancelledBooking = bookingService.cancelBooking(id, reason);

        return ResponseEntity.ok(BookingMapper.toCancellationDto(cancelledBooking, policyResult));
    }


    @Operation(summary = "Get booking rooms", description = "Retrieve the list of rooms associated with a specific booking by its ID.")
    @GetMapping("/{id}/rooms")
    public List<BookingRoomResponseDto> getBookingRooms(@PathVariable Long id) {
        return bookingService.getBookingById(id).getBookingRooms().stream().map(BookingRoomMapper::toDto).collect(toList());
    }

    @Operation(summary = "Get booking by reference", description = "Retrieve a booking using its unique booking reference code. This allows users to find their booking without needing the booking ID.")
    @GetMapping("/reference/{reference}")
    public BookingResponseDto getBookingByReference(@PathVariable String reference) {
        return BookingMapper.toDto(bookingService.findBookingByReference(reference));
    }


    @PutMapping("/{id}")
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
    public ResponseEntity<BookingResponseDto> checkInBooking(@PathVariable Long id) {
        Booking checkedIn = bookingService.checkInBooking(id);
        return ResponseEntity.ok(BookingMapper.toDto(checkedIn));
    }

        @PutMapping("/{id}/checkout")
    public ResponseEntity<BookingResponseDto> checkOutBooking(@PathVariable Long id) {
        Booking checkedOut = bookingService.checkOutBooking(id);
        return ResponseEntity.ok(BookingMapper.toDto(checkedOut));
}
}
