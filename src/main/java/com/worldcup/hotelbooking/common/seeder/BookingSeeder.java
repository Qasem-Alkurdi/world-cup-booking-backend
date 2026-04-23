package com.worldcup.hotelbooking.common.seeder;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import com.worldcup.hotelbooking.reservation.booking.Booking;
import com.worldcup.hotelbooking.reservation.booking.BookingRepository;
import com.worldcup.hotelbooking.reservation.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.user.AppUser;
import com.worldcup.hotelbooking.user.AppUserRepository;
import com.worldcup.hotelbooking.user.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Order(6)
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class BookingSeeder implements CommandLineRunner {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final AppUserRepository appUserRepository;

    private final Random random = new Random(12345);

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("BOOKING SEEDER STARTED");
        log.info("BOOKING SEEDER STARTED");

        if (bookingRepository.count() > 0) {
            log.info("Bookings already exist. Skipping booking seeder.");
            return;
        }

        List<Hotel> hotels = hotelRepository.findAll();
        List<AppUser> guests = appUserRepository.findAll().stream()
                .filter(user -> user.getRoles() != null && user.getRoles().contains(Role.GUEST))
                .toList();

        if (hotels.isEmpty() || guests.isEmpty()) {
            log.warn("No hotels or guests found. Skipping booking seeder.");
            return;
        }

        List<Booking> bookings = new ArrayList<>();

        // Scenario 1: Opening match - Mexico City
        bookings.add(createBooking(
                guests.get(0),
                findHotelByCity(hotels, "Ciudad de Mexico"),
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 15),
                2, 2, 0,
                Booking.BookingStatus.CONFIRMED,
                false,
                "Opening match weekend in Mexico City"
        ));

        // Scenario 2: Pending booking
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                hotels.get(randomIndex(hotels.size())),
                LocalDate.of(2026, 6, 20),
                LocalDate.of(2026, 6, 24),
                2, 2, 0,
                Booking.BookingStatus.PENDING,
                false,
                "Pending payment"
        ));

        // Scenario 3: Checked in
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Atlanta"),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(3),
                2, 2, 0,
                Booking.BookingStatus.CHECKED_IN,
                false,
                "Currently checked in"
        ));

        // Scenario 4: Checked out
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Foxborough"),
                LocalDate.now().minusDays(7),
                LocalDate.now().minusDays(3),
                2, 2, 0,
                Booking.BookingStatus.CHECKED_OUT,
                false,
                "Completed stay"
        ));

        // Scenario 5: Cancelled
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Arlington"),
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 15),
                2, 2, 0,
                Booking.BookingStatus.CANCELLED,
                false,
                "User cancelled - personal reasons"
        ));

        // Scenario 6: Confirmed with additional payment required
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Inglewood"),
                LocalDate.of(2026, 7, 8),
                LocalDate.of(2026, 7, 12),
                4, 2, 2,
                Booking.BookingStatus.CONFIRMED,
                true,
                "Price increased after booking modification"
        ));

        // Scenario 7-12: Group stage bookings
        String[] cities = {"Houston", "Seattle", "Toronto", "Vancouver", "Apodaca", "Atlanta"};
        for (int i = 0; i < cities.length; i++) {
            LocalDate checkIn = LocalDate.of(2026, 6, 14 + (i * 2));
            bookings.add(createBooking(
                    guests.get(randomIndex(guests.size())),
                    findHotelByCity(hotels, cities[i]),
                    checkIn,
                    checkIn.plusDays(3),
                    random.nextInt(3) + 2,
                    2,
                    random.nextInt(2),
                    Booking.BookingStatus.CONFIRMED,
                    false,
                    "Group stage match week in " + cities[i]
            ));
        }

        // Extra Miami-area booking using actual seeded city
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Miami Gardens"),
                LocalDate.of(2026, 6, 26),
                LocalDate.of(2026, 6, 29),
                3, 2, 1,
                Booking.BookingStatus.CONFIRMED,
                false,
                "Hard Rock Stadium stay"
        ));

        // Scenario 13-16: Round of 32
        for (int i = 0; i < 4; i++) {
            LocalDate checkIn = LocalDate.of(2026, 6, 28).plusDays(i);
            bookings.add(createBooking(
                    guests.get(randomIndex(guests.size())),
                    hotels.get(randomIndex(hotels.size())),
                    checkIn,
                    checkIn.plusDays(2),
                    random.nextInt(3) + 1,
                    random.nextInt(2) + 1,
                    random.nextInt(2),
                    Booking.BookingStatus.CONFIRMED,
                    false,
                    "Round of 32 knockout stage"
            ));
        }

        // Scenario 17-20: Quarter-finals
        LocalDate[] quarterFinalDates = {
                LocalDate.of(2026, 7, 13),
                LocalDate.of(2026, 7, 14)
        };

        for (LocalDate date : quarterFinalDates) {
            bookings.add(createBooking(
                    guests.get(randomIndex(guests.size())),
                    findHotelByCity(hotels, "Arlington"),
                    date.minusDays(1),
                    date.plusDays(2),
                    random.nextInt(3) + 2,
                    2,
                    random.nextInt(2),
                    Booking.BookingStatus.CONFIRMED,
                    false,
                    "Quarter-final weekend in Arlington"
            ));

            bookings.add(createBooking(
                    guests.get(randomIndex(guests.size())),
                    findHotelByCity(hotels, "Inglewood"),
                    date.minusDays(1),
                    date.plusDays(2),
                    random.nextInt(3) + 2,
                    2,
                    random.nextInt(2),
                    Booking.BookingStatus.CONFIRMED,
                    false,
                    "Quarter-final weekend in Inglewood"
            ));
        }

        // Scenario 21-22: Semi-finals
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Arlington"),
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 7, 18),
                2, 2, 0,
                Booking.BookingStatus.CONFIRMED,
                false,
                "Semi-final 1 at AT&T Stadium"
        ));

        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Inglewood"),
                LocalDate.of(2026, 7, 16),
                LocalDate.of(2026, 7, 19),
                4, 2, 2,
                Booking.BookingStatus.CONFIRMED,
                false,
                "Semi-final 2 at SoFi Stadium"
        ));

        // Scenario 23: Final
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Carlstadt"),
                LocalDate.of(2026, 7, 18),
                LocalDate.of(2026, 7, 21),
                4, 2, 2,
                Booking.BookingStatus.CONFIRMED,
                false,
                "World Cup Final at MetLife Stadium"
        ));

        // Scenario 24-28: Family bookings
        for (int i = 0; i < 5; i++) {
            LocalDate checkIn = LocalDate.of(2026, 6, 16 + (i * 3));
            bookings.add(createBooking(
                    guests.get(randomIndex(guests.size())),
                    hotels.get(randomIndex(hotels.size())),
                    checkIn,
                    checkIn.plusDays(5),
                    6,
                    4,
                    2,
                    Booking.BookingStatus.CONFIRMED,
                    false,
                    "Family vacation during World Cup"
            ));
        }

        // Scenario 29-30: Recent cancellations
        for (int i = 0; i < 2; i++) {
            Booking cancelled = createBooking(
                    guests.get(randomIndex(guests.size())),
                    hotels.get(randomIndex(hotels.size())),
                    LocalDate.of(2026, 6, 25 + i),
                    LocalDate.of(2026, 6, 28 + i),
                    2, 2, 0,
                    Booking.BookingStatus.CANCELLED,
                    false,
                    "Cancelled - schedule conflict"
            );
            cancelled.setCancelledAt(LocalDateTime.now().minusDays(i + 1));
            cancelled.setCancelledBy(cancelled.getAppUser().getUsername());
            bookings.add(cancelled);
        }
// Scenario 31-38: Extra checked-out bookings for review seeding
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Atlanta"),
                LocalDate.now().minusDays(20),
                LocalDate.now().minusDays(16),
                2, 2, 0,
                Booking.BookingStatus.CHECKED_OUT,
                false,
                "Completed stay in Atlanta for review seeding"
        ));

        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Seattle"),
                LocalDate.now().minusDays(18),
                LocalDate.now().minusDays(14),
                2, 2, 0,
                Booking.BookingStatus.CHECKED_OUT,
                false,
                "Completed stay in Seattle for review seeding"
        ));

        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Toronto"),
                LocalDate.now().minusDays(17),
                LocalDate.now().minusDays(13),
                2, 2, 0,
                Booking.BookingStatus.CHECKED_OUT,
                false,
                "Completed stay in Toronto for review seeding"
        ));

        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Vancouver"),
                LocalDate.now().minusDays(16),
                LocalDate.now().minusDays(12),
                2, 2, 0,
                Booking.BookingStatus.CHECKED_OUT,
                false,
                "Completed stay in Vancouver for review seeding"
        ));

        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Inglewood"),
                LocalDate.now().minusDays(15),
                LocalDate.now().minusDays(11),
                2, 2, 0,
                Booking.BookingStatus.CHECKED_OUT,
                false,
                "Completed stay in Inglewood for review seeding"
        ));

        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Arlington"),
                LocalDate.now().minusDays(14),
                LocalDate.now().minusDays(10),
                2, 2, 0,
                Booking.BookingStatus.CHECKED_OUT,
                false,
                "Completed stay in Arlington for review seeding"
        ));

        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Ciudad de Mexico"),
                LocalDate.now().minusDays(13),
                LocalDate.now().minusDays(9),
                2, 2, 0,
                Booking.BookingStatus.CHECKED_OUT,
                false,
                "Completed stay in Mexico City for review seeding"
        ));

        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Apodaca"),
                LocalDate.now().minusDays(12),
                LocalDate.now().minusDays(8),
                2, 2, 0,
                Booking.BookingStatus.CHECKED_OUT,
                false,
                "Completed stay in Apodaca for review seeding"
        ));
        bookingRepository.saveAll(bookings);
        log.info("Seeded {} bookings with realistic World Cup scenarios.", bookings.size());
    }

    private Booking createBooking(
            AppUser guest,
            Hotel hotel,
            LocalDate checkIn,
            LocalDate checkOut,
            int totalGuests,
            int adults,
            int children,
            Booking.BookingStatus status,
            boolean requiresAdditionalPayment,
            String notes
    ) {
        Booking booking = new Booking();

        // important: prevents NullPointerException if list is not initialized in entity
        booking.setBookingRooms(new ArrayList<>());

        booking.setAppUser(guest);
        booking.setHotel(hotel);
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setNumberOfGuests(totalGuests);
        booking.setNumberOfAdults(adults);
        booking.setNumberOfChildren(children);
        booking.setStatus(status);
        booking.setAdditionalPaymentRequired(requiresAdditionalPayment);
        booking.setActive(true);

        LocalDateTime now = LocalDateTime.now();
        if (status == Booking.BookingStatus.CONFIRMED
                || status == Booking.BookingStatus.CHECKED_IN
                || status == Booking.BookingStatus.CHECKED_OUT) {
            booking.setConfirmedAt(now.minusDays(random.nextInt(30)));
        }

        List<RoomType> hotelRooms = roomTypeRepository.findByHotelId(hotel.getId());
        if (!hotelRooms.isEmpty()) {
            addBookingRooms(booking, hotelRooms, totalGuests);
        }

        booking.setTotalPrice(calculateEstimatedPrice(booking));

        if (requiresAdditionalPayment) {
            booking.setAdditionalPaymentRequired(true);
        }

        return booking;
    }

    private void addBookingRooms(Booking booking, List<RoomType> availableRooms, int totalGuests) {
        int roomsNeeded = (int) Math.ceil(totalGuests / 2.0);

        RoomType selectedRoomType;
        if (totalGuests <= 2 && availableRooms.size() >= 1) {
            selectedRoomType = availableRooms.get(0);
        } else if (totalGuests >= 4 && availableRooms.size() >= 3) {
            selectedRoomType = availableRooms.get(2);
        } else {
            selectedRoomType = availableRooms.get(Math.min(1, availableRooms.size() - 1));
        }

        BookingRoom bookingRoom = new BookingRoom();
        bookingRoom.setBooking(booking);
        bookingRoom.setRoomType(selectedRoomType);
        bookingRoom.setNumberOfRooms(roomsNeeded);
        bookingRoom.setBasePricePerNightPerRoom(selectedRoomType.getBasePrice());

        int nights = (int) ChronoUnit.DAYS.between(
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );

        BigDecimal roomTotal = selectedRoomType.getBasePrice()
                .multiply(BigDecimal.valueOf(nights))
                .multiply(BigDecimal.valueOf(roomsNeeded))
                .multiply(BigDecimal.valueOf(1.15));

        bookingRoom.setTotalPriceWithFees(roomTotal);
        booking.getBookingRooms().add(bookingRoom);
    }

    private BigDecimal calculateEstimatedPrice(Booking booking) {
        BigDecimal total = BigDecimal.ZERO;
        for (BookingRoom room : booking.getBookingRooms()) {
            total = total.add(room.getTotalPriceWithFees());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private Hotel findHotelByCity(List<Hotel> hotels, String cityName) {
        return hotels.stream()
                .filter(h -> h.getCity() != null && h.getCity().equalsIgnoreCase(cityName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hotel found for city: " + cityName));
    }

    private int randomIndex(int size) {
        return random.nextInt(size);
    }
}