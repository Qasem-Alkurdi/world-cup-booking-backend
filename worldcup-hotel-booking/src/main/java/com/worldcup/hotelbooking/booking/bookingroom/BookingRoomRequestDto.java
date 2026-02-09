package com.worldcup.hotelbooking.booking.bookingroom;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class BookingRoomRequestDto {
    private int numberOfRooms;
    private long roomTypeId;

}
