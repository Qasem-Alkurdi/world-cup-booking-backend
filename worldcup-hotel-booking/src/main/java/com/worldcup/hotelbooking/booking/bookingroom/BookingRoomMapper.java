package com.worldcup.hotelbooking.booking.bookingroom;

import com.worldcup.hotelbooking.booking.booking.*;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;

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
        BookingRoomResponseDto dto = new BookingRoomResponseDto();
        dto.setRoomTypeName(entity.getRoomType().getName());
        dto.setNumberOfRooms(entity.getNumberOfRooms());
        dto.setPricePerNight(entity.getPricePerNight());

        return dto;
    }
}
