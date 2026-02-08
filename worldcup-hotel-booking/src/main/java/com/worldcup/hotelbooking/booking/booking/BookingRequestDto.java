package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRequestDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

    @Data
    public class BookingRequestDto {

        @NotBlank(message = "Hotel ID cannot be null")
        private Long hotelId;
        @NotBlank(message = "User ID cannot be null")
        private long userId;

        private Long matchId;

        @NotBlank(message = "Check-in date cannot be null")
        private LocalDate checkInDate;

        @NotBlank(message = "Check-out date cannot be null")
        private LocalDate checkOutDate;

        @Min(1)
        private int numberOfGuests;
        private int numberOfAdults;
        private int numberOfChildren;

        private List<BookingRoomRequestDto> rooms;
    }


