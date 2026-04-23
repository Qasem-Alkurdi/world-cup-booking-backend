package com.worldcup.hotelbooking.chat;

import com.worldcup.hotelbooking.user.AppUser;
import com.worldcup.hotelbooking.user.Role;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A single text message inside a Conversation.
 * <p>
 * senderRole tells the frontend which side of the chat bubble to render —
 * GUEST = right, MANAGER = left — without the frontend needing to compare ids.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private AppUser sender;


    @Column(name = "sender_role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role senderRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    /**
     * Flipped to true when the recipient loads the conversation.
     */
    @Column(nullable = false)
    private boolean read = false;

    public ChatMessage(Conversation conversation, AppUser sender,
                       Role senderRole, String content) {
        this.conversation = conversation;
        this.sender = sender;
        this.senderRole = senderRole;
        this.content = content;
    }

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }

    public enum SenderRole {
        GUEST,
        MANAGER
    }
}