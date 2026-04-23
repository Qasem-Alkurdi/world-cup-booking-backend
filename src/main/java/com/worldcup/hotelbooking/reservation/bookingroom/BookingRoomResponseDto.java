package com.worldcup.hotelbooking.reservation.bookingroom;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@JsonPropertyOrder({
        "roomTypeName",
        "numberOfRooms",
        "basePricePerNight",
        "totalPriceWithFees"
})
//this annotation specifies the order in which the properties of the BookingRoomResponseDto class will be serialized to JSON. When an instance of this class is converted to JSON, the properties will appear in the specified order: roomTypeName, numberOfRooms, basePricePerNight, and totalPriceWithFees.
public class BookingRoomResponseDto {

    private String roomTypeName;
    private int numberOfRooms;
    private BigDecimal basePricePerNight;
    private BigDecimal totalPriceWithFees;

    BookingRoomResponseDto(String roomTypeName, int numberOfRooms, BigDecimal pricePerNight, BigDecimal totalPriceWithFees) {
        this.roomTypeName = roomTypeName;
        this.numberOfRooms = numberOfRooms;
        this.basePricePerNight = pricePerNight;
        this.totalPriceWithFees = totalPriceWithFees;
    }
}

