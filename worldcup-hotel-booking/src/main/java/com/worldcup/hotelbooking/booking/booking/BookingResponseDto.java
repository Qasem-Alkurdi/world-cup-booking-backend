package com.worldcup.hotelbooking.booking.booking;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomResponseDto;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@JsonPropertyOrder({
        "id",
        "bookingReference",
        "status",
        "checkInDate",
        "checkOutDate",
        "numberOfGuests",
        "numberOfAdults",
        "numberOfChildren",
        "confirmationDeadLine",
        "totalPrice",
        "additionalPaymentRequired",
        "rooms"
})//this annotation specifies the order in which the properties of the BookingResponseDto class will be serialized to JSON. When an instance of this class is converted to JSON, the properties will appear in the specified order: bookingReference, status, checkInDate, checkOutDate, confirmationDeadLine, totalPrice, amountPaid, AdditionalPaymentAmount, and rooms.
public class BookingResponseDto {

    BookingResponseDto(Long Id,String bookingReference, Booking.BookingStatus status,int numberOfGuests,int numberOfAdults,int numberOfChildren, LocalDate checkInDate, LocalDate checkOutDate,LocalDateTime confirmationDeadLine, BigDecimal totalPrice,boolean additionalPaymentRequired,
                     List<BookingRoomResponseDto> rooms) {
        this.id=Id;
        this.bookingReference = bookingReference;
        this.status = status;
        this.numberOfGuests = numberOfGuests;
        this.numberOfAdults = numberOfAdults;
        this.numberOfChildren = numberOfChildren;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.totalPrice = totalPrice;
        this.rooms = rooms;
        this.confirmationDeadLine = confirmationDeadLine;
        this.additionalPaymentRequired = additionalPaymentRequired;
    }

    private Long id;
    private String bookingReference;
    private Booking.BookingStatus status;

    private int numberOfGuests;
    private int numberOfAdults;
    private int numberOfChildren;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;


    private LocalDateTime confirmationDeadLine;

    private BigDecimal totalPrice;
    private boolean additionalPaymentRequired;


    private List<BookingRoomResponseDto> rooms=new ArrayList<>();

//    public void setRooms(List<BookingRoomResponseDto> bookingRoomsResponseDto) {
//        this.rooms = bookingRoomsResponseDto;
//    }
}
