package com.worldcup.hotelbooking.chat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Returned when a user opens the chat panel on a hotel page.
 * Contains full message history and unread count for the requesting user.
 */
public record ConversationResponse(

        Long conversationId,
        Long hotelId,
        String hotelName,
        String guestUsername,
        LocalDateTime createdAt,
        long unreadCount,
        List<ChatMessageResponse> messages
) {
    public static ConversationResponse from(Conversation conversation,
                                            long unreadCount,
                                            List<ChatMessageResponse> messages) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getHotel().getId(),
                conversation.getHotel().getName(),
                conversation.getGuest().getUsername(),
                conversation.getCreatedAt(),
                unreadCount,
                messages
        );
    }
}