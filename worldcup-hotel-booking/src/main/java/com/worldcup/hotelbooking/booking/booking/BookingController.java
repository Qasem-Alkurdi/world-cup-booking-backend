package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelService;
import com.worldcup.hotelbooking.user.user.AppUser;
import com.worldcup.hotelbooking.user.user.AppUserNotFoundException;
import com.worldcup.hotelbooking.user.user.AppUserServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bookings")
public class BookingController {
        private final BookingService bookingService;
        private final AppUserServiceImpl appUserServiceImpl;
        private final HotelService hotelService;

        BookingController(BookingService bookingService, AppUserServiceImpl appUserServiceImpl, HotelService hotelService) {
            this.hotelService = hotelService;
            this.appUserServiceImpl = appUserServiceImpl;
            this.bookingService = bookingService;
        }

        //get

    @GetMapping("/{id}")
    public BookingResponseDto getBookingById(@PathVariable Long id) {
        return BookingMapper.toDto(bookingService.getBookingById(id));
    }

    @GetMapping("/user/{userId}/status/{status}")
    public List<BookingResponseDto> getUserBookingsByStatus(@PathVariable Long userId, @PathVariable String status) {
        return bookingService.getUserBookings(userId, status).stream().map(BookingMapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/user/{userId}")
    public List<BookingResponseDto> getUserBookings(@PathVariable Long userId) {
        return bookingService.getUserBookings(userId).stream().map(BookingMapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/hotel/{hotelId}/status/{status}")
    public List<BookingResponseDto> getHotelBookingsByStatus(@PathVariable Long hotelId, @PathVariable String status) {
        return bookingService.getHotelBookings(hotelId, status).stream().map(BookingMapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/hotel/{hotelId}")
    public List<BookingResponseDto> getHotelBookings(@PathVariable Long hotelId) {
        return bookingService.getHotelBookings(hotelId).stream().map(BookingMapper::toDto).collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody BookingRequestDto bookingRequest,
            UriComponentsBuilder uriBuilder) {


        AppUser user = appUserServiceImpl.getUserById(bookingRequest.getUserId());



        Hotel hotel = hotelService.getHotelById(bookingRequest.getHotelId());


        Booking booking = BookingMapper.toEntity(bookingRequest, user, hotel);


        Booking createdBooking = bookingService.createBooking(booking);


        BookingResponseDto responseDto = BookingMapper.toDto(createdBooking);


        return ResponseEntity.created(
                uriBuilder.path("/bookings/{id}")
                        .buildAndExpand(createdBooking.getId())
                        .toUri()
        ).body(responseDto);
    }


    @PutMapping("/{id}/confirm")
    public ResponseEntity<BookingResponseDto> updateBookingStatus(@PathVariable Long id, @RequestParam String status) {
        Booking updatedBooking = bookingService.confirmBooking(id);
        return ResponseEntity.ok(BookingMapper.toDto(updatedBooking));
    }

    @PutMapping("/{id}/cancel/{reason}")
    public ResponseEntity<BookingResponseDto> cancelBooking(@PathVariable Long id, @PathVariable String reason) {
        Booking updatedBooking = bookingService.cancelBooking(id, reason);
        return ResponseEntity.ok(BookingMapper.toDto(updatedBooking));
    }
}
