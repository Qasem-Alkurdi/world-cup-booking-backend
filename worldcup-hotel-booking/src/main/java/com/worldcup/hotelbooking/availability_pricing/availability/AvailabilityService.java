package com.worldcup.hotelbooking.availability_pricing.availability;

import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRepository;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import com.worldcup.hotelbooking.catalog.roomtype.exceptions.RoomTypeNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AvailabilityService {
    private final RoomTypeRepository roomTypeRepository;
    private final BookingRoomRepository bookingRoomRepository;

    AvailabilityService(RoomTypeRepository roomTypeRepository, BookingRoomRepository bookingRoomRepository) {
        this.bookingRoomRepository = bookingRoomRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    // @GetMapping("/availability/room-type/{id} ?checkIn=2026-06-10 &checkOut=2026-06-12"")
    public boolean checkRoomTypeAvailability(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return roomTypeRepository.findById(roomTypeId).orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId)).getTotalRooms() - bookingRoomRepository.countBookedRooms(roomTypeId, checkIn, checkOut) > 0;
    }

    public int getAvailableRooms(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return roomTypeRepository.findById(roomTypeId).orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId)).getTotalRooms() - bookingRoomRepository.countBookedRooms(roomTypeId, checkIn, checkOut);
    }

    public boolean isNumberOfGuestsValid(Booking booking) {
        int numberOfValidAdults = 0;
        int numberOfValidChildren = 0;
        for (BookingRoom room : booking.getBookingRooms()) {
            numberOfValidAdults += room.getRoomType().getMaxAdults() * room.getNumberOfRooms();
            numberOfValidChildren += room.getRoomType().getMaxChildren() * room.getNumberOfRooms();
        }
        return booking.getNumberOfAdults() <= numberOfValidAdults && booking.getNumberOfChildren() <= numberOfValidChildren;
    }

    public boolean checkAvailability(Long roomTypeId, java.time.LocalDate checkIn, java.time.LocalDate checkOut, int rooms) {
        int bookedRooms = bookingRoomRepository.countBookedRooms(roomTypeId, checkIn, checkOut);
        int availableRooms =
                roomTypeRepository.findById(roomTypeId)
                        .orElseThrow(() -> new IllegalArgumentException("Room type not found with id: " + roomTypeId))
                        .getTotalRooms()
                        - bookedRooms;

        if (availableRooms < rooms) {
            return false;
        }
        return true;
    }


}
