package com.worldcup.hotelbooking.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Full history for a conversation, oldest first.
     */
    List<ChatMessage> findByConversationIdOrderBySentAtAsc(Long conversationId);

    /**
     * Count messages sent by the OTHER party that this user has not read yet.
     */
    @Query("""
            SELECT COUNT(m)
            FROM ChatMessage m
            WHERE m.conversation.id = :conversationId
              AND m.sender.id      != :userId
              AND m.read            = false
            """)
    long countUnreadForUser(@Param("conversationId") Long conversationId,
                            @Param("userId") Long userId);

    /**
     * Mark all messages sent by the OTHER party as read when this user opens the chat.
     */
    @Modifying
    @Query("""
            UPDATE ChatMessage m
            SET m.read = true
            WHERE m.conversation.id = :conversationId
              AND m.sender.id      != :userId
              AND m.read            = false
            """)
    void markAllReadForUser(@Param("conversationId") Long conversationId,
                            @Param("userId") Long userId);
}