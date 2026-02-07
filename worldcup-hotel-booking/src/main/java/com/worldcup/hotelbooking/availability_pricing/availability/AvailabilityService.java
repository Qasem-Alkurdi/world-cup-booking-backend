package com.worldcup.hotelbooking.availability_pricing.availability;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRepository;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeNotFoundException;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
      return roomTypeRepository.findById(roomTypeId).orElseThrow(()-> new RoomTypeNotFoundException("Can not find Room Type With id"+roomTypeId)).getNumberOfRooms()-bookingRoomRepository.countBookedRooms(roomTypeId, checkIn, checkOut) > 0;
    }

    public int getAvailableRooms(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        return roomTypeRepository.findById(roomTypeId).orElseThrow(()-> new RoomTypeNotFoundException("Can not find Room Type With id"+roomTypeId)).getNumberOfRooms()-bookingRoomRepository.countBookedRooms(roomTypeId, checkIn, checkOut);
    }
}
