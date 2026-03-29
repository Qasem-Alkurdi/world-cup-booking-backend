package com.worldcup.hotelbooking.common.seeder;

import com.worldcup.hotelbooking.chat.ChatMessage;
import com.worldcup.hotelbooking.chat.ChatMessageRepository;
import com.worldcup.hotelbooking.chat.Conversation;
import com.worldcup.hotelbooking.chat.ConversationRepository;
import com.worldcup.hotelbooking.user.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ChatMessageSeeder - Creates realistic chat messages between guests and hotel managers
 *
 * Message scenarios:
 * - Booking inquiries
 * - Check-in time questions
 * - Room amenity questions
 * - Special requests (early check-in, late check-out, extra beds)
 * - Parking and transportation questions
 * - Restaurant and area recommendations
 * - Payment and cancellation policy questions
 */
@Component
@Order(9)
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class ChatMessageSeeder implements CommandLineRunner {

    private final ChatMessageRepository chatMessageRepository;
    private final ConversationRepository conversationRepository;

    private final Random random = new Random(11111); // Fixed seed

    @Override
    @Transactional
    public void run(String... args) {
        if (chatMessageRepository.count() > 0) {
            log.info("Chat messages already exist. Skipping chat message seeder.");
            return;
        }

        List<Conversation> conversations = conversationRepository.findAll();
        if (conversations.isEmpty()) {
            log.warn("No conversations found. Run ConversationSeeder first.");
            return;
        }

        List<ChatMessage> allMessages = new ArrayList<>();

        // Add messages to random conversations (not all conversations have messages)
        int conversationsWithMessages = Math.min(conversations.size(), 15); // Limit to 15 conversations

        for (int i = 0; i < conversationsWithMessages; i++) {
            Conversation conversation = conversations.get(i);
            List<ChatMessage> messages = createConversationMessages(conversation);
            allMessages.addAll(messages);
        }

        chatMessageRepository.saveAll(allMessages);
        log.info("Seeded {} chat messages across {} conversations.",
                allMessages.size(), conversationsWithMessages);
    }

    private List<ChatMessage> createConversationMessages(Conversation conversation) {
        List<ChatMessage> messages = new ArrayList<>();

        // Pick a random conversation scenario
        int scenario = random.nextInt(8);

        switch (scenario) {
            case 0 -> messages.addAll(createBookingInquiryConversation(conversation));
            case 1 -> messages.addAll(createCheckInTimeConversation(conversation));
            case 2 -> messages.addAll(createEarlyCheckInConversation(conversation));
            case 3 -> messages.addAll(createParkingConversation(conversation));
            case 4 -> messages.addAll(createAmenityConversation(conversation));
            case 5 -> messages.addAll(createCancellationPolicyConversation(conversation));
            case 6 -> messages.addAll(createRestaurantRecommendationConversation(conversation));
            case 7 -> messages.addAll(createAdditionalGuestConversation(conversation));
        }

        return messages;
    }

    // Scenario 1: Booking Inquiry
    private List<ChatMessage> createBookingInquiryConversation(Conversation conversation) {
        List<ChatMessage> messages = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(random.nextInt(10) + 5);

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime,
                "Hello! I'm interested in booking a room for the World Cup matches in June. Do you have availability for June 14-17?",
                false));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(15),
                "Hello! Thank you for your interest. Yes, we have rooms available for those dates. We have Standard, Deluxe, and Suite options. Would you like me to provide pricing details?",
                true));

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime.plusMinutes(30),
                "Yes please! I'll need a room for 2 adults. What would you recommend?",
                true));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(45),
                "For 2 adults, I'd recommend our Deluxe King Room. It features a comfortable king bed, workspace, and all modern amenities. The rate for June 14-17 (3 nights) would be approximately $850 total. Shall I reserve this for you?",
                true));

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime.plusHours(2),
                "That sounds perfect! Yes, please proceed with the reservation.",
                true));

        return messages;
    }

    // Scenario 2: Check-in Time Question
    private List<ChatMessage> createCheckInTimeConversation(Conversation conversation) {
        List<ChatMessage> messages = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(random.nextInt(5) + 1);

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime,
                "Hi! What time is check-in? My flight arrives at 11 AM.",
                false));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(10),
                "Hello! Our standard check-in time is 3:00 PM. However, if you'd like to check in earlier, we can arrange early check-in for an additional fee of $50, subject to availability. Would you like me to reserve this for you?",
                true));

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime.plusMinutes(25),
                "Yes please! I'd like to confirm early check-in at 11:30 AM.",
                true));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(35),
                "Perfect! I've noted early check-in at 11:30 AM for your reservation. The $50 fee will be added to your final bill. Looking forward to welcoming you!",
                true));

        return messages;
    }

    // Scenario 3: Early Check-in Request
    private List<ChatMessage> createEarlyCheckInConversation(Conversation conversation) {
        List<ChatMessage> messages = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(random.nextInt(7) + 2);

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime,
                "Hello! Is it possible to check in earlier than 3 PM? I'll be arriving around 10 AM.",
                false));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(20),
                "Good morning! Yes, we can accommodate early check-in. There's a $50 fee for guaranteed early check-in. Alternatively, you're welcome to store your luggage with us free of charge and explore the area until your room is ready. Which would you prefer?",
                true));

        return messages;
    }

    // Scenario 4: Parking Question
    private List<ChatMessage> createParkingConversation(Conversation conversation) {
        List<ChatMessage> messages = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(random.nextInt(8) + 3);

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime,
                "Hi, do you have parking available? I'll be driving to the stadium.",
                false));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(12),
                "Hello! Yes, we have on-site parking available. It's $25 per night for self-parking, or $40 per night for valet parking. The parking garage is very convenient - just a 2-minute walk to the hotel entrance. Would you like me to pre-reserve a parking spot for you?",
                true));

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime.plusMinutes(30),
                "Yes, please reserve self-parking for me. Also, how far is it to the stadium from your hotel?",
                true));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(45),
                "Perfect! I've reserved self-parking for your stay. The stadium is approximately 1.5 miles from our hotel - about a 10-minute drive or 30-minute walk. We also have a shuttle service to the stadium on match days for $10 per person round trip. Let me know if you'd like to book that as well!",
                true));

        return messages;
    }

    // Scenario 5: Amenity Questions
    private List<ChatMessage> createAmenityConversation(Conversation conversation) {
        List<ChatMessage> messages = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(random.nextInt(6) + 2);

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime,
                "Hi! Does the room have a mini fridge? I need to keep some drinks cold.",
                false));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(8),
                "Hello! Yes, all our Deluxe and Suite rooms come with a mini refrigerator. Our Standard rooms don't have one by default, but we can provide a mini fridge upon request for $15 per night. Which room type did you book?",
                true));

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime.plusMinutes(20),
                "I booked a Standard room. I'd like to add the mini fridge please!",
                true));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(30),
                "Great! I've added a mini fridge to your room. The $15/night fee will appear on your final bill. Is there anything else you need for your stay?",
                true));

        return messages;
    }

    // Scenario 6: Cancellation Policy
    private List<ChatMessage> createCancellationPolicyConversation(Conversation conversation) {
        List<ChatMessage> messages = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(random.nextInt(15) + 10);

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime,
                "Hello, what is your cancellation policy? My plans might change.",
                false));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(15),
                "Hello! Our cancellation policy during World Cup season is as follows:\n\n" +
                        "• 30+ days before check-in: 100% refund\n" +
                        "• 14-29 days: 75% refund\n" +
                        "• 7-13 days: 50% refund\n" +
                        "• 3-6 days: 25% refund\n" +
                        "• Less than 3 days: No refund\n\n" +
                        "Please note that these policies are stricter than usual due to the high demand during the tournament. Let me know if you have any other questions!",
                true));

        return messages;
    }

    // Scenario 7: Restaurant Recommendations
    private List<ChatMessage> createRestaurantRecommendationConversation(Conversation conversation) {
        List<ChatMessage> messages = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(random.nextInt(4) + 1);

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime,
                "Hi! Can you recommend any good restaurants nearby for dinner?",
                false));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(18),
                "Hello! We have several excellent options nearby:\n\n" +
                        "• Our in-house restaurant serves contemporary American cuisine\n" +
                        "• The Steakhouse (2 blocks away) - Upscale dining, reservations recommended\n" +
                        "• Joe's Pizza (walking distance) - Best pizza in the area\n" +
                        "• Sushi World (5 minutes drive) - Fresh sushi and Japanese cuisine\n\n" +
                        "Would you like me to make a reservation for you at any of these?",
                true));

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime.plusMinutes(35),
                "The Steakhouse sounds great! Can you make a reservation for 2 people at 7 PM tomorrow?",
                true));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(50),
                "Absolutely! I've made a reservation for 2 guests at The Steakhouse tomorrow at 7:00 PM under your name. Enjoy your dinner!",
                true));

        return messages;
    }

    // Scenario 8: Additional Guest
    private List<ChatMessage> createAdditionalGuestConversation(Conversation conversation) {
        List<ChatMessage> messages = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(random.nextInt(5) + 2);

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime,
                "Hello, I need to add one more person to my booking. Is that possible?",
                false));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(10),
                "Hello! Yes, we can accommodate an additional guest. May I ask which room type you booked? Standard rooms accommodate up to 2 guests, while Deluxe can fit up to 3, and Suites up to 4. There may be a small additional guest fee depending on your room type.",
                true));

        messages.add(createMessage(conversation, conversation.getGuest(),
                ChatMessage.SenderRole.GUEST, baseTime.plusMinutes(25),
                "I booked a Deluxe room, so that should work. What's the additional fee?",
                true));

        messages.add(createMessage(conversation, conversation.getHotel().getOwner(),
                ChatMessage.SenderRole.MANAGER, baseTime.plusMinutes(40),
                "Perfect! For a third guest in a Deluxe room, there's an additional $30 per night. I'll update your booking to include 3 guests. The additional charge will be reflected in your total. Is there anything else you need?",
                true));

        return messages;
    }

    private ChatMessage createMessage(
            Conversation conversation,
            AppUser sender,
            ChatMessage.SenderRole role,
            LocalDateTime sentAt,
            String content,
            boolean read
    ) {
        ChatMessage message = new ChatMessage(conversation, sender, role, content);
        message.setSentAt(sentAt);
        message.setRead(read);
        return message;
    }
}