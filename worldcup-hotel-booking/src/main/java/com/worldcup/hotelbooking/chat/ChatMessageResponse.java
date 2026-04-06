package com.worldcup.hotelbooking.chat;

import java.time.LocalDateTime;

/**
 * Sent to the frontend both via REST (history load) and via WebSocket (live push).
 * Using the same DTO for both keeps the frontend code simple.
 */
public record ChatMessageResponse(

        Long id,
        Long conversationId,
        Long senderId,
        String senderUsername,
        com.worldcup.hotelbooking.user.Role senderRole,
        String content,
        LocalDateTime sentAt,
        boolean read
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getSenderRole(),
                message.getContent(),
                message.getSentAt(),
                message.isRead()
        );
    }
}