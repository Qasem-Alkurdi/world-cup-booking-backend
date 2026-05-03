package com.worldcup.hotelbooking.availability_pricing.availability;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import com.worldcup.hotelbooking.catalog.roomtype.exception.RoomTypeNotFoundException;
import com.worldcup.hotelbooking.reservation.booking.Booking;
import com.worldcup.hotelbooking.reservation.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.reservation.bookingroom.BookingRoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AvailabilityServiceImpl {
    private final RoomTypeRepository roomTypeRepository;
    private final BookingRoomRepository bookingRoomRepository;

    AvailabilityServiceImpl(RoomTypeRepository roomTypeRepository, BookingRoomRepository bookingRoomRepository) {
        this.bookingRoomRepository = bookingRoomRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    // @GetMapping("/availability/room-type/{id} ?checkIn=2026-06-10 &checkOut=2026-06-12"")
    @Transactional
    public boolean checkRoomTypeAvailability(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isAfter(checkOut))
            throw new IllegalArgumentException("Check in Date can not be after check date ");
        if (checkIn.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("you can not enter past date ");
        return roomTypeRepository.findById(roomTypeId).orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId)).getTotalRooms() - bookingRoomRepository.countBookedRooms(roomTypeId, checkIn, checkOut) > 0;
    }

    @Transactional
    public int getAvailableRooms(Long roomTypeId, LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isAfter(checkOut))
            throw new IllegalArgumentException("Check in Date can not be after check date ");
        if (checkIn.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("you can not enter past date ");
        return roomTypeRepository.findById(roomTypeId).orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId)).getTotalRooms() - bookingRoomRepository.countBookedRooms(roomTypeId, checkIn, checkOut);
    }

    @Transactional
    public boolean isNumberOfGuestsValid(Booking booking) {
        int numberOfValidAdults = 0;
        int numberOfValidChildren = 0;
        for (BookingRoom room : booking.getBookingRooms()) {
            numberOfValidAdults += room.getRoomType().getMaxAdults() * room.getNumberOfRooms();
            numberOfValidChildren += room.getRoomType().getMaxChildren() * room.getNumberOfRooms();
        }
        return booking.getNumberOfAdults() <= numberOfValidAdults && booking.getNumberOfChildren() <= numberOfValidChildren;
    }

    @Transactional
    public boolean checkAvailability(Long roomTypeId, java.time.LocalDate checkIn, java.time.LocalDate checkOut, int rooms) {
        if (checkOut.isBefore(checkIn))
            throw new IllegalArgumentException("The check in  date can not be after the check out date");
        if (checkIn.isAfter(checkOut))
            throw new IllegalArgumentException("Check in Date can not be after check date ");
        if (checkIn.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("you can not enter past date ");
        int bookedRooms = bookingRoomRepository.countBookedRooms(roomTypeId, checkIn, checkOut);
        int availableRooms =
                roomTypeRepository.findById(roomTypeId)
                        .orElseThrow(() -> new IllegalArgumentException("Room type not found with id: " + roomTypeId))
                        .getTotalRooms()
                        - bookedRooms;

        return availableRooms >= rooms;
    }


    @Transactional
    public boolean checkAvailabilityOfHotel(Hotel hotel, LocalDate checkIn, LocalDate checkout) {
        if (checkout.isBefore(checkIn))
            throw new IllegalArgumentException("The check in  date can not be after the check out date");
        if (checkIn.isAfter(checkout))
            throw new IllegalArgumentException("Check in Date can not be after check date ");
        if (checkIn.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("you can not enter past date ");
        boolean b = false;
        for (RoomType roomType : hotel.getRoomTypes()) {
            if (checkAvailability(roomType.getId(), checkIn, checkout, 1))
                b = true;
        }
        return b;
    }

}

