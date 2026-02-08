package com.worldcup.hotelbooking.booking.bookingroom;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookingRoomRequestDto {
    private int numberOfRooms;
    private long roomTypeId;

}
