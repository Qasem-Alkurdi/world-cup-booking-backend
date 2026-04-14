package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRequestDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class BookingRequestDto {

    @NotNull(message = "Hotel ID cannot be null")
    @Positive(message = "Hotel ID must be positive")
    private Long hotelId;


    @NotNull(message = "Check-in date cannot be null")
    private LocalDate checkInDate;

    @NotNull(message = "Check-out date cannot be null")
    private LocalDate checkOutDate;

    @Min(1)
    private int numberOfGuests;
    private int numberOfAdults;
    private int numberOfChildren;

    private List<BookingRoomRequestDto> rooms;
}