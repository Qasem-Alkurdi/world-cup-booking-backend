package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomMapper;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRequestDto;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomResponseDto;
import com.worldcup.hotelbooking.catalog.hotel.HotelService;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeService;
import com.worldcup.hotelbooking.user.user.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private final BookingServiceImp bookingService;
    private final AppUserService appUserService;
    private final HotelService hotelService;
    private final RoomTypeService roomTypeService;

    BookingController(BookingServiceImp bookingService, AppUserService appUserService, HotelService hotelService, RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
        this.hotelService = hotelService;
        this.appUserService = appUserService;
        this.bookingService = bookingService;
    }

    //get

    @GetMapping("/{id}")
    public BookingResponseDto getBookingById(@PathVariable Long id) {
        return BookingMapper.toDto(bookingService.getBookingById(id));
    }

    @GetMapping("/user/{userId}/status/{status}")
    public List<BookingResponseDto> getUserBookingsByStatus(@PathVariable Long userId, @PathVariable Booking.BookingStatus status) {
        return bookingService.getUserBookings(userId, status).stream().map(BookingMapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/user/{userId}")
    public List<BookingResponseDto> getUserBookings(@PathVariable Long userId) {
        return bookingService.getUserBookings(userId).stream().map(BookingMapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/hotel/{hotelId}/status/{status}")
    public List<BookingResponseDto> getHotelBookingsByStatus(@PathVariable Long hotelId, @PathVariable Booking.BookingStatus status) {
        return bookingService.getHotelBookings(hotelId, status).stream().map(BookingMapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/hotel/{hotelId}")
    public List<BookingResponseDto> getHotelBookings(@PathVariable Long hotelId) {
        return bookingService.getHotelBookings(hotelId).stream().map(BookingMapper::toDto).collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(@Valid @RequestBody BookingRequestDto bookingRequest, UriComponentsBuilder uriBuilder) {
        Booking booking = BookingMapper.toEntity(bookingRequest, appUserService.getUserById(bookingRequest.getUserId()), hotelService.findById(bookingRequest.getHotelId()));
        for (BookingRoomRequestDto roomRequest : bookingRequest.getRooms()) {
            bookingService.addBookingRoom(
                    BookingRoomMapper.toEntity(
                            roomRequest, booking, roomTypeService.findById(bookingRequest.getHotelId(), roomRequest.getRoomTypeId())
                    )
            );
        }

        Booking createdBooking = bookingService.createBooking(booking);

        BookingResponseDto responseDto = BookingMapper.toDto(createdBooking);
        for (BookingRoom bookingRoom : createdBooking.getBookingRooms()) {
            responseDto.getRooms().add(BookingRoomMapper.toDto(bookingRoom));
        }
        return ResponseEntity.created(uriBuilder.path("/bookings/{id}").buildAndExpand(createdBooking.getId()).toUri()).body(responseDto);
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<BookingResponseDto> updateBookingStatus(@PathVariable Long id) {
        Booking updatedBooking = bookingService.confirmBooking(id);
        return ResponseEntity.ok(BookingMapper.toDto(updatedBooking));
    }

    @PutMapping("/{id}/cancel/{reason}")
    public ResponseEntity<BookingResponseDto> cancelBooking(@PathVariable Long id, @PathVariable String reason) {
        Booking updatedBooking = bookingService.cancelBooking(id, reason);
        return ResponseEntity.ok(BookingMapper.toDto(updatedBooking));
    }

    @GetMapping("/{id}/rooms")
    public List<BookingRoomResponseDto> getBookingRooms(@PathVariable Long id) {
        return bookingService.getBookingById(id).getBookingRooms().stream().map(BookingRoomMapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/reference/{reference}")
    public BookingResponseDto getBookingByReference(@PathVariable String reference) {
        return BookingMapper.toDto(bookingService.findBookingByReference(reference));
    }


}
