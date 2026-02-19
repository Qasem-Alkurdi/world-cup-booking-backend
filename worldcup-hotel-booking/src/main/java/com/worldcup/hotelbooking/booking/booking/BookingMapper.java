package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomMapper;
import com.worldcup.hotelbooking.booking.cancellation.CancellationResult;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.user.user.AppUser;

public class BookingMapper {

    public static Booking toEntity(
            BookingRequestDto dto,
            AppUser appUser,
            Hotel hotel) {

        Booking booking = new Booking();
        booking.setAppUser(appUser);
        booking.setHotel(hotel);
        booking.setCheckInDate(dto.getCheckInDate());
        booking.setCheckOutDate(dto.getCheckOutDate());
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setNumberOfGuests(dto.getNumberOfGuests());
        booking.setNumberOfAdults(dto.getNumberOfAdults());
        booking.setNumberOfChildren(dto.getNumberOfChildren());

        return booking;
    }

    public static BookingResponseDto toDto(Booking booking) {
        BookingResponseDto dto = new BookingResponseDto(
                booking.getBookingReference(),
                booking.getStatus(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getTotalPrice(),
                booking.getBookingRooms().stream()
                        .map(BookingRoomMapper::toDto)
                        .toList()
        );

        return dto;
    }

    public static BookingCancellationResponse toCancellationDto(Booking cancelledBooking, CancellationResult policyResult) {
        return new BookingCancellationResponse(
                BookingMapper.toDto(cancelledBooking),
                policyResult.getRefundAmount(),
                policyResult.getCancellationFee(),
                policyResult.getRefundPercentage(),
                policyResult.getPolicyMessage()
        );
    }

}
