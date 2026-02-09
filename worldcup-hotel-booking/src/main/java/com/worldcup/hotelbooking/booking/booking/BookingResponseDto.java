package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomResponseDto;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
public class BookingResponseDto {

    BookingResponseDto(String bookingReference, Booking.BookingStatus status, LocalDate checkInDate, LocalDate checkOutDate, BigDecimal totalPrice, List<BookingRoomResponseDto> rooms) {
        this.bookingReference = bookingReference;
        this.status = status;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.totalPrice = totalPrice;
        this.rooms = rooms;

    }

    private String bookingReference;
    private Booking.BookingStatus status;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    private BigDecimal totalPrice;

    private List<BookingRoomResponseDto> rooms;
}
