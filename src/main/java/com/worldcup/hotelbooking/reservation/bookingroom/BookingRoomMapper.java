package com.worldcup.hotelbooking.reservation.bookingroom;

import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.reservation.booking.Booking;

public class BookingRoomMapper {
    public static BookingRoom toEntity(
            BookingRoomRequestDto dto,
            Booking booking,
            RoomType roomType) {

        BookingRoom br = new BookingRoom();
        br.setBooking(booking);
        br.setRoomType(roomType);
        br.setNumberOfRooms(dto.getNumberOfRooms());

        return br;
    }

    public static BookingRoomResponseDto toDto(BookingRoom entity) {
        BookingRoomResponseDto dto = new BookingRoomResponseDto(
                entity.getRoomType().getName(),
                entity.getNumberOfRooms(),
                entity.getRoomType().getBasePrice(),
                entity.getTotalPriceWithFees()
        );

        return dto;
    }
}
