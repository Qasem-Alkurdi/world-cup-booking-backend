package com.worldcup.hotelbooking.chat;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotel.exception.HotelNotFoundException;
import com.worldcup.hotelbooking.user.AppUser;
import com.worldcup.hotelbooking.user.AppUserNotFoundException;
import com.worldcup.hotelbooking.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository  chatMessageRepository;
    private final HotelRepository        hotelRepository;
    private final AppUserRepository      appUserRepository;
    private final SimpMessagingTemplate  messagingTemplate;

    public ChatServiceImpl(ConversationRepository conversationRepository,
                           ChatMessageRepository chatMessageRepository,
                           HotelRepository hotelRepository,
                           AppUserRepository appUserRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository  = chatMessageRepository;
        this.hotelRepository        = hotelRepository;
        this.appUserRepository      = appUserRepository;
        this.messagingTemplate      = messagingTemplate;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GUEST: open/load their conversation with a hotel
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ConversationResponse getOrCreateGuestConversation(Long hotelId, Long guestId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        AppUser guest = appUserRepository.findById(guestId)
                .orElseThrow(() -> new AppUserNotFoundException("User not found with id: " + guestId));

        Conversation conversation = conversationRepository
                .findByGuestIdAndHotelId(guestId, hotelId)
                .orElseGet(() -> {
                    logger.info("Creating new conversation: guest={} hotel={}", guestId, hotelId);
                    return conversationRepository.save(new Conversation(guest, hotel));
                });

        chatMessageRepository.markAllReadForUser(conversation.getId(), guestId);

        return buildResponse(conversation, guestId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GUEST: send a message (creates conversation lazily if first message)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ChatMessageResponse guestSendMessage(Long hotelId, Long guestId, String content) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        AppUser guest = appUserRepository.findById(guestId)
                .orElseThrow(() -> new AppUserNotFoundException("User not found with id: " + guestId));

        Conversation conversation = conversationRepository
                .findByGuestIdAndHotelId(guestId, hotelId)
                .orElseGet(() -> {
                    logger.info("Creating new conversation on first message: guest={} hotel={}", guestId, hotelId);
                    return conversationRepository.save(new Conversation(guest, hotel));
                });

        return saveAndPublish(conversation, guest, ChatMessage.SenderRole.GUEST, content);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER: load a specific conversation by id
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ConversationResponse getConversationById(Long conversationId, Long managerId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        chatMessageRepository.markAllReadForUser(conversationId, managerId);

        return buildResponse(conversation, managerId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER: reply to a specific conversation
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ChatMessageResponse managerSendMessage(Long conversationId, Long managerId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        AppUser manager = appUserRepository.findById(managerId)
                .orElseThrow(() -> new AppUserNotFoundException("User not found with id: " + managerId));

        return saveAndPublish(conversation, manager, ChatMessage.SenderRole.MANAGER, content);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Persists the message and broadcasts it via WebSocket to all subscribers
     * of /topic/conversation/{conversationId}.
     */
    private ChatMessageResponse saveAndPublish(Conversation conversation,
                                               AppUser sender,
                                               ChatMessage.SenderRole role,
                                               String content) {
        ChatMessage saved = chatMessageRepository.save(
                new ChatMessage(conversation, sender, role, content));

        ChatMessageResponse response = ChatMessageResponse.from(saved);

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + conversation.getId(), response);//this is the topic the frontend subscribes to for live updates

        logger.info("Message sent in conversation {} by {} ({})",
                conversation.getId(), sender.getUsername(), role);

        return response;
    }

    private ConversationResponse buildResponse(Conversation conversation, Long userId) {
        long unreadCount = chatMessageRepository
                .countUnreadForUser(conversation.getId(), userId);

        List<ChatMessageResponse> messages = chatMessageRepository
                .findByConversationIdOrderBySentAtAsc(conversation.getId())
                .stream()
                .map(ChatMessageResponse::from)
                .toList();

        return ConversationResponse.from(conversation, unreadCount, messages);
    }
}