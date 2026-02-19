package com.worldcup.hotelbooking.booking.bookingroom;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingRoomServiceImp implements BookingRoomService {
    private final BookingRoomRepository bookingRoomRepository;
    BookingRoomServiceImp(BookingRoomRepository bookingRoomRepository) {
        this.bookingRoomRepository = bookingRoomRepository;
    }
    @Override
    public List<BookingRoom> getAllBookingRooms() {
        return bookingRoomRepository.findAll();
    }
}
