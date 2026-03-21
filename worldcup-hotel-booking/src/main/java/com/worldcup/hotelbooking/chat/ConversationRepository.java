package com.worldcup.hotelbooking.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * The core lookup — one conversation per guest+hotel pair.
     * Used by the "find or create" pattern in ChatServiceImpl.
     */
    Optional<Conversation> findByGuestIdAndHotelId(Long guestId, Long hotelId);

    /**
     * Verify that a user is a participant of a conversation —
     * either the guest or the hotel owner.
     * Used by BookingAuthorizationService to protect chat endpoints.
     */
    @Query("""
            SELECT COUNT(c) > 0
            FROM Conversation c
            WHERE c.id           = :conversationId
              AND (
                    c.guest.id         = :userId
                 OR c.hotel.owner.id   = :userId
              )
            """)
    boolean isParticipant(@Param("conversationId") Long conversationId,
                          @Param("userId") Long userId);
}