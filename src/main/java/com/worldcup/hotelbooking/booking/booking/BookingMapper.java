package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomMapper;
import com.worldcup.hotelbooking.booking.cancellation.CancellationResponse;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.user.AppUser;

import java.math.BigDecimal;

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
                booking.getId(),
                booking.getBookingReference(),
                booking.getStatus(),
                booking.getNumberOfGuests(),
                booking.getNumberOfAdults(),
                booking.getNumberOfChildren(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getConfirmationDeadline(),
                booking.getTotalPrice(),
                booking.isAdditionalPaymentRequired(),
                booking.getBookingRooms().stream()
                        .map(BookingRoomMapper::toDto)
                        .toList()
        );
        return dto;
    }


    public static BookingCancellationResponse toCancellationDto(
            Booking cancelledBooking,
            CancellationResponse policyResult) {

        return new BookingCancellationResponse(
                BookingMapper.toDto(cancelledBooking),
                policyResult.getRefundAmount(),
                policyResult.getCancellationFee(),
                policyResult.getRefundPercentage(),
                policyResult.getPolicyMessage(),
                null,                                    // bonusAmount       — null for guest cancel
                null,                                    // bonusTierDescription — null for guest cancel
                policyResult.getTotalPayout(),           // totalPayout = refundAmount (no bonus)
                cancelledBooking.getCancelledBy()        // cancelledBy — guest username
        );
    }


    public static BookingCancellationResponse toManagerCancellationDto(
            Booking cancelledBooking,
            CancellationResponse policyResult) {

        return new BookingCancellationResponse(
                BookingMapper.toDto(cancelledBooking),
                policyResult.getRefundAmount(),          // base refund (100% of totalPrice)
                BigDecimal.ZERO,                         // cancellationFee — always 0 for manager cancel
                100,                                     // refundPercentage — always 100% base
                policyResult.getPolicyMessage(),
                policyResult.getBonusAmount(),           // bonusAmount
                policyResult.getBonusTierDescription(),  // bonusTierDescription
                policyResult.getTotalPayout(),           // totalPayout = base + bonus
                cancelledBooking.getCancelledBy()        // cancelledBy — manager username
        );
    }


}
