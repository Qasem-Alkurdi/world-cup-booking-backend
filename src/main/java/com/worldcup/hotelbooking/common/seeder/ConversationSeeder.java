package com.worldcup.hotelbooking.common.seeder;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.chat.Conversation;
import com.worldcup.hotelbooking.chat.ConversationRepository;
import com.worldcup.hotelbooking.reservation.booking.Booking;
import com.worldcup.hotelbooking.reservation.booking.BookingRepository;
import com.worldcup.hotelbooking.user.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ConversationSeeder - Creates chat conversations between guests and hotels
 * <p>
 * Creates one conversation per guest-hotel pair for bookings
 */
@Component
@Order(8)
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class ConversationSeeder implements CommandLineRunner {

    private final ConversationRepository conversationRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (conversationRepository.count() > 0) {
            log.info("Conversations already exist. Skipping conversation seeder.");
            return;
        }

        List<Booking> bookings = bookingRepository.findAll();
        if (bookings.isEmpty()) {
            log.warn("No bookings found. Run BookingSeeder first.");
            return;
        }

        // Track unique guest-hotel pairs to avoid duplicates
        Map<String, Conversation> guestHotelPairs = new HashMap<>();
        List<Conversation> conversations = new ArrayList<>();

        for (Booking booking : bookings) {
            AppUser guest = booking.getAppUser();
            Hotel hotel = booking.getHotel();

            String pairKey = guest.getId() + "-" + hotel.getId();

            if (!guestHotelPairs.containsKey(pairKey)) {
                Conversation conversation = new Conversation(guest, hotel);
                conversations.add(conversation);
                guestHotelPairs.put(pairKey, conversation);
            }
        }

        conversationRepository.saveAll(conversations);
        log.info("Seeded {} conversations (unique guest-hotel pairs).", conversations.size());
    }
}