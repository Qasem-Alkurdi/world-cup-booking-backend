package com.worldcup.hotelbooking.common.seeder;

import com.worldcup.hotelbooking.availability_pricing.match.Match;
import com.worldcup.hotelbooking.availability_pricing.match.MatchRepository;
import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.booking.booking.BookingRepository;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.roomtype.RoomType;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import com.worldcup.hotelbooking.user.user.AppUser;
import com.worldcup.hotelbooking.user.user.AppUserRepository;
import com.worldcup.hotelbooking.user.user.Role;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * BookingSeeder - Creates realistic booking scenarios for World Cup 2026
 *
 * Scenarios included:
 * - CONFIRMED bookings with completed payments
 * - PENDING bookings awaiting payment
 * - CHECKED_IN guests currently staying
 * - CHECKED_OUT completed stays
 * - CANCELLED bookings with refunds
 * - Bookings with additional payment required (price changes)
 * - Bookings during major matches (higher prices)
 */
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
    private final MatchRepository matchRepository;

    private final Random random = new Random(12345); // Fixed seed for reproducibility

    @Override
    @Transactional
    public void run(String... args) {
        if (bookingRepository.count() > 0) {
            log.info("Bookings already exist. Skipping booking seeder.");
            return;
        }

        List<Hotel> hotels = hotelRepository.findAll();
        List<AppUser> guests = appUserRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(Role.GUEST))
                .toList();
        List<Match> matches = matchRepository.findAll();

        if (hotels.isEmpty() || guests.isEmpty()) {
            log.warn("No hotels or guests found. Skipping booking seeder.");
            return;
        }

        List<Booking> bookings = new ArrayList<>();

        // Scenario 1: CONFIRMED booking during opening match (Mexico City) - June 11
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

        // Scenario 2: PENDING booking - awaiting payment
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

        // Scenario 3: CHECKED_IN - currently staying
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

        // Scenario 4: CHECKED_OUT - completed stay
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Boston"),
                LocalDate.now().minusDays(7),
                LocalDate.now().minusDays(3),
                2, 2, 0,
                Booking.BookingStatus.CHECKED_OUT,
                false,
                "Completed stay"
        ));

        // Scenario 5: CANCELLED with refund
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Dallas"),
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 15),
                2, 2, 0,
                Booking.BookingStatus.CANCELLED,
                false,
                "User cancelled - personal reasons"
        ));

        // Scenario 6: CONFIRMED with additional payment required (price increased)
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Los Angeles"),
                LocalDate.of(2026, 7, 8),
                LocalDate.of(2026, 7, 12),
                4, 2, 2,
                Booking.BookingStatus.CONFIRMED,
                true, // Additional payment required
                "Price increased after booking modification"
        ));

        // Scenario 7-12: Group stage bookings across different cities
        String[] cities = {"Houston", "Miami", "Seattle", "Toronto", "Vancouver", "Monterrey"};
        for (int i = 0; i < cities.length; i++) {
            LocalDate checkIn = LocalDate.of(2026, 6, 14 + (i * 2));
            bookings.add(createBooking(
                    guests.get(randomIndex(guests.size())),
                    findHotelByCity(hotels, cities[i]),
                    checkIn,
                    checkIn.plusDays(3),
                    random.nextInt(3) + 2, // 2-4 guests
                    2,
                    random.nextInt(2), // 0-1 children
                    Booking.BookingStatus.CONFIRMED,
                    false,
                    "Group stage match week in " + cities[i]
            ));
        }

        // Scenario 13-16: Round of 32 bookings (late June/early July)
        for (int i = 0; i < 4; i++) {
            LocalDate checkIn = LocalDate.of(2026, 6, 28 + i);
            bookings.add(createBooking(
                    guests.get(randomIndex(guests.size())),
                    hotels.get(randomIndex(hotels.size())),
                    checkIn,
                    checkIn.plusDays(2),
                    random.nextInt(3) + 1, // 1-3 guests
                    random.nextInt(2) + 1, // 1-2 adults
                    random.nextInt(2), // 0-1 children
                    Booking.BookingStatus.CONFIRMED,
                    false,
                    "Round of 32 knockout stage"
            ));
        }

        // Scenario 17-20: Quarter-finals bookings (premium matches, higher demand)
        LocalDate[] quarterFinalDates = {
                LocalDate.of(2026, 7, 13),
                LocalDate.of(2026, 7, 14)
        };
        for (LocalDate date : quarterFinalDates) {
            // Two bookings per quarter-final date
            for (int j = 0; j < 2; j++) {
                bookings.add(createBooking(
                        guests.get(randomIndex(guests.size())),
                        findHotelByCity(hotels, j == 0 ? "Dallas" : "Los Angeles"),
                        date.minusDays(1),
                        date.plusDays(2),
                        random.nextInt(3) + 2, // 2-4 guests
                        2,
                        random.nextInt(2),
                        Booking.BookingStatus.CONFIRMED,
                        false,
                        "Quarter-final match weekend"
                ));
            }
        }

        // Scenario 21-22: Semi-finals bookings (very premium)
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Dallas"), // AT&T Stadium
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 7, 18),
                2, 2, 0,
                Booking.BookingStatus.CONFIRMED,
                false,
                "Semi-final 1 at AT&T Stadium"
        ));

        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "Los Angeles"), // SoFi Stadium
                LocalDate.of(2026, 7, 16),
                LocalDate.of(2026, 7, 19),
                4, 2, 2,
                Booking.BookingStatus.CONFIRMED,
                false,
                "Semi-final 2 at SoFi Stadium"
        ));

        // Scenario 23: Final booking (most premium)
        bookings.add(createBooking(
                guests.get(randomIndex(guests.size())),
                findHotelByCity(hotels, "New York/New Jersey"), // MetLife Stadium
                LocalDate.of(2026, 7, 18),
                LocalDate.of(2026, 7, 21),
                4, 2, 2,
                Booking.BookingStatus.CONFIRMED,
                false,
                "World Cup Final at MetLife Stadium"
        ));

        // Scenario 24-28: Family bookings (larger groups, multiple rooms)
        for (int i = 0; i < 5; i++) {
            LocalDate checkIn = LocalDate.of(2026, 6, 16 + (i * 3));
            bookings.add(createBooking(
                    guests.get(randomIndex(guests.size())),
                    hotels.get(randomIndex(hotels.size())),
                    checkIn,
                    checkIn.plusDays(5),
                    6, // Large family
                    4, // 4 adults
                    2, // 2 children
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

        // Set timestamps based on status
        LocalDateTime now = LocalDateTime.now();
        if (status == Booking.BookingStatus.CONFIRMED ||
                status == Booking.BookingStatus.CHECKED_IN ||
                status == Booking.BookingStatus.CHECKED_OUT) {
            booking.setConfirmedAt(now.minusDays(random.nextInt(30)));
        }

        // Add booking rooms
        List<RoomType> hotelRooms = roomTypeRepository.findByHotelId(hotel.getId());
        if (!hotelRooms.isEmpty()) {
            addBookingRooms(booking, hotelRooms, totalGuests);
        }

        // Calculate total price (will be updated by pricing service in real app)
        BigDecimal totalPrice = calculateEstimatedPrice(booking, hotel);
        booking.setTotalPrice(totalPrice);

        if (requiresAdditionalPayment) {
            booking.setAdditionalPaymentRequired(true);
        }

        return booking;
    }

    private void addBookingRooms(Booking booking, List<RoomType> availableRooms, int totalGuests) {
        // Determine how many rooms needed
        int roomsNeeded = (int) Math.ceil(totalGuests / 2.0); // Assume 2 guests per room average

        // Pick room type (prefer Standard for smaller groups, Suite for larger)
        RoomType selectedRoomType;
        if (totalGuests <= 2 && availableRooms.size() >= 1) {
            selectedRoomType = availableRooms.get(0); // Standard
        } else if (totalGuests >= 4 && availableRooms.size() >= 3) {
            selectedRoomType = availableRooms.get(2); // Suite
        } else {
            selectedRoomType = availableRooms.get(Math.min(1, availableRooms.size() - 1)); // Deluxe
        }

        BookingRoom bookingRoom = new BookingRoom();
        bookingRoom.setBooking(booking);
        bookingRoom.setRoomType(selectedRoomType);
        bookingRoom.setNumberOfRooms(roomsNeeded);
        bookingRoom.setBasePricePerNightPerRoom(selectedRoomType.getBasePrice());

        // Estimated price (pricing service will calculate exact)
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(
                booking.getCheckInDate(), booking.getCheckOutDate());
        BigDecimal roomTotal = selectedRoomType.getBasePrice()
                .multiply(BigDecimal.valueOf(nights))
                .multiply(BigDecimal.valueOf(roomsNeeded))
                .multiply(BigDecimal.valueOf(1.15)); // 15% markup estimate

        bookingRoom.setTotalPriceWithFees(roomTotal);

        booking.getBookingRooms().add(bookingRoom);
    }

    private BigDecimal calculateEstimatedPrice(Booking booking, Hotel hotel) {
        BigDecimal total = BigDecimal.ZERO;
        for (BookingRoom room : booking.getBookingRooms()) {
            total = total.add(room.getTotalPriceWithFees());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private Hotel findHotelByCity(List<Hotel> hotels, String cityName) {
        return hotels.stream()
                .filter(h -> h.getCity().equalsIgnoreCase(cityName) ||
                        h.getCity().contains(cityName))
                .findFirst()
                .orElse(hotels.get(0)); // Fallback to first hotel
    }

    private int randomIndex(int size) {
        return random.nextInt(size);
    }
}