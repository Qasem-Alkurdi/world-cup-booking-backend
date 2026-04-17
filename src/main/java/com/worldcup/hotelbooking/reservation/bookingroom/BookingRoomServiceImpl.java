package com.worldcup.hotelbooking.reservation.bookingroom;

import org.springframework.stereotype.Service;

@Service
public class BookingRoomServiceImpl implements BookingRoomService {
    private final BookingRoomRepository bookingRoomRepository;

    BookingRoomServiceImpl(BookingRoomRepository bookingRoomRepository) {
        this.bookingRoomRepository = bookingRoomRepository;
    }

}
