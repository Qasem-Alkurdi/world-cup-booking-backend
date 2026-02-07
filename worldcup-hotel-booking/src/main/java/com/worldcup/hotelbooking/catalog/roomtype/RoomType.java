package com.worldcup.hotelbooking.catalog.roomtype;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity

@Getter
@Setter
public class RoomType {

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private int MaxAdults;
    private int MaxChildren;
    private int numberOfRooms;

@OneToMany(mappedBy = "roomType")
@JsonManagedReference
    private List<BookingRoom> bookingRooms;

public void addBookingRoom(BookingRoom bookingRoom) {
    this.bookingRooms.add(bookingRoom);
    bookingRoom.setRoomType(this);
}

    public Long getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public int getMaxAdults() {
        return MaxAdults;
    }
    public int getMaxChildren() {
        return MaxChildren;
    }
    public int getNumberOfRooms() {
        return numberOfRooms;
    }
}
