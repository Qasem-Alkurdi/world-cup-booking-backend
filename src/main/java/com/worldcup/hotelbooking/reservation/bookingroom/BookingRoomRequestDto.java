package com.worldcup.hotelbooking.reservation.bookingroom;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingRoomRequestDto {

    private long roomTypeId;
    private int numberOfRooms;

}
