package com.worldcup.hotelbooking.booking.bookingroom;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class BookingRoomResponseDto {

    BookingRoomResponseDto(String roomTypeName, int numberOfRooms, BigDecimal pricePerNight) {
        this.roomTypeName = roomTypeName;
        this.numberOfRooms = numberOfRooms;
        this.pricePerNight = pricePerNight;
    }
    private String roomTypeName;
    private int numberOfRooms;
    private BigDecimal pricePerNight;
}

