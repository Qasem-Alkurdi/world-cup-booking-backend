package com.worldcup.hotelbooking.chat;


public interface ChatService {

    // ── GUEST flows ──────────────────────────────────────────────────────────

    /**
     * Guest opens the chat panel on a hotel page.
     * Creates the conversation lazily if this is the first visit.
     * Marks the other party's messages as read.
     */
    ConversationResponse getOrCreateGuestConversation(Long hotelId, Long guestId);

    /**
     * Guest sends a message to a hotel.
     * Creates the conversation lazily if this is the first message.
     * Persists and pushes via WebSocket.
     */
    ChatMessageResponse guestSendMessage(Long hotelId, Long guestId, String content);

    // ── MANAGER flows ─────────────────────────────────────────────────────────

    /**
     * Manager opens a specific conversation (identified by conversationId).
     * Marks the guest's messages as read.
     */
    ConversationResponse getConversationById(Long conversationId, Long managerId);

    /**
     * Manager replies in a specific conversation.
     * Persists and pushes via WebSocket.
     */
    ChatMessageResponse managerSendMessage(Long conversationId, Long managerId, String content);
}