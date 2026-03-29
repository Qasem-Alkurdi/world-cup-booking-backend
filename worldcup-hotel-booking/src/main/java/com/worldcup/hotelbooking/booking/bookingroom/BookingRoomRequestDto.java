package com.worldcup.hotelbooking.booking.bookingroom;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BookingRoomRequestDto {

    private long roomTypeId;
    private int numberOfRooms;

}
