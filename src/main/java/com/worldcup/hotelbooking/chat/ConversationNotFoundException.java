package com.worldcup.hotelbooking.chat;

public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(Long conversationId) {
        super("Conversation not found with id: " + conversationId);
    }
}