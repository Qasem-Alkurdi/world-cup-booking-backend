package com.worldcup.hotelbooking.catalog.roomtype.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RoomTypeAvailabilityCriteria {
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer adults;
    private Integer children;
    private Integer numberOfRooms;
}