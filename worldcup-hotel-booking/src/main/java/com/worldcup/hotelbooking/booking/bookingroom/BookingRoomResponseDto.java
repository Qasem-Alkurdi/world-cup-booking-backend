package com.worldcup.hotelbooking.booking.bookingroom;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookingRoomResponseDto {

    private String roomTypeName;
    private int numberOfRooms;
    private BigDecimal pricePerNight;
}

