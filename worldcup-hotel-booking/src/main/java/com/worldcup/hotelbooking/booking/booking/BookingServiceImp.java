package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRepository;
import com.worldcup.hotelbooking.catalog.hotel.HotelNotFoundException;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import com.worldcup.hotelbooking.user.user.UserNotFoundException;
import com.worldcup.hotelbooking.user.user.UserRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class BookingServiceImp implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingRoomRepository bookingRoomRepository;

    BookingServiceImp(BookingRepository bookingRepository, UserRepository userRepository, HotelRepository hotelRepository, RoomTypeRepository roomTypeRepository, BookingRoomRepository bookingRoomRepository) {
        this.userRepository = userRepository;
        this.hotelRepository = hotelRepository;
        this.bookingRepository = bookingRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.bookingRoomRepository = bookingRoomRepository;
    }
    //get
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(Long userId, String status) {
        if(!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        return bookingRepository.findByUserIdAndStatus(userId, status);
    }

    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(Long userId) {
        if(!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        return bookingRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Booking> getHotelBookings(Long hotelId, String status) {
        if(!hotelRepository.existsById(hotelId)) {
            throw new HotelNotFoundException("Hotel not found with id: " + hotelId);
        }
        return bookingRepository.findByHotelIdAndStatus(hotelId, status);
    }

    @Transactional(readOnly = true)
    public List<Booking> getHotelBookings(Long hotelId) {
        if(!hotelRepository.existsById(hotelId)) {
            throw new HotelNotFoundException("Hotel not found with id: " + hotelId);
        }
        return bookingRepository.findByHotelId(hotelId);
    }
    /////////////////////////////////////////////////////////////

    //create

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)//to run all the code in this method as a single transaction and to prevent dirty reads, non-repeatable reads, and phantom reads, ensuring data integrity during the booking process.
    public Booking createBooking(Booking booking) {
        booking.setTotalPrice(calculateTotalPrice(booking, booking.getCheckInDate(), booking.getCheckOutDate()));
        booking.setStatus("PENDING");
        if(booking.getCheckOutDate().isBefore(booking.getCheckInDate())) {
            throw new IllegalArgumentException("Check-out date cannot be before check-in date");
        }
        if(booking.getBookingRooms() == null || booking.getBookingRooms().isEmpty()) {
            throw new IllegalArgumentException("At least one room must be booked");
        }
            if(!isNumberOfGuestsValid(booking)) {
                throw new IllegalArgumentException("Number of guests exceeds room capacity");
            }
            for(BookingRoom room : booking.getBookingRooms()) {
                if(!checkAvailability(room.getRoomType().getId(), booking.getCheckInDate(), booking.getCheckOutDate(), room.getNumberOfRooms())) {
                    throw new IllegalArgumentException("Not enough rooms available for room type: " + room.getRoomType().getName());
                }
            }

        booking.getHotel().getBookings().add(booking);
        booking.getUser().getBookings().add(booking);
        return bookingRepository.save(booking);
    }

    public BigDecimal calculateTotalPrice(Booking booking,java.time.LocalDate checkIn, java.time.LocalDate checkOut) {
        long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal total = BigDecimal.ZERO;
        for (BookingRoom room : booking.getBookingRooms()) {
            total = total.add(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)).multiply(BigDecimal.valueOf(room.getNumberOfRooms())));
        }
        return total;
    }

    public boolean checkAvailability(Long roomTypeId, java.time.LocalDate checkIn, java.time.LocalDate checkOut, int rooms) {
            int bookedRooms = bookingRoomRepository.countBookedRooms(roomTypeId, checkIn, checkOut);
            int availableRooms = roomTypeRepository.findById(roomTypeId).orElseThrow(() -> new IllegalArgumentException("Room type not found with id: " + roomTypeId)).getNumberOfRooms()- bookedRooms;
            if (availableRooms < rooms) {
                return false;
            }
        return true;
    }

    public boolean isNumberOfGuestsValid(Booking booking) {
        int numberOfValidAdults=0;
        int numberOfValidChildren=0;
        for(BookingRoom room : booking.getBookingRooms()) {
           numberOfValidAdults+=room.getRoomType().getMaxAdults() * room.getNumberOfRooms();
           numberOfValidChildren+=room.getRoomType().getMaxChildren() * room.getNumberOfRooms();
        }
        return booking.getNumberOfAdults() <= numberOfValidAdults && booking.getNumberOfChildren() <= numberOfValidChildren;
    }

    @Override
    public Booking cancelBooking(Long Id, String reason) {
        Booking booking = bookingRepository.findById(Id).orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + Id));
        if(booking.getStatus().equals("CANCELLED")) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        booking.setStatus("CANCELLED");
        booking.setCancelReason(reason);
        booking.setCancelledAt(java.time.LocalDate.now());
        booking.setCancelledBy(booking.getUser().getName());
        return bookingRepository.save(booking);
    }

    @Override
    public Booking confirmBooking(Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
        if(booking.getStatus().equals("CONFIRMED")) {
            throw new IllegalStateException("Booking is already confirmed");
        }
        if(booking.getStatus().equals("CANCELLED")) {
            throw new IllegalStateException("Cancelled booking cannot be confirmed");
        }
        booking.setStatus("CONFIRMED");
        booking.setConfirmedAt(java.time.LocalDate.now());
        return bookingRepository.save(booking);
    }


}
