package com.worldcup.hotelbooking.chat;

import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.user.user.AppUser;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One conversation per guest–hotel pair.
 * Created lazily when either party sends the first message or opens the chat panel.
 *
 * Authorization:
 *   - guest   = conversation.guest
 *   - manager = conversation.hotel.owner
 */
@Data
@NoArgsConstructor
@Entity
@Table(
        name = "conversation",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_conversation_guest_hotel",
                columnNames = {"guest_id", "hotel_id"}
        )
)
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private AppUser guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt ASC")
    private List<ChatMessage> messages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Conversation(AppUser guest, Hotel hotel) {
        this.guest = guest;
        this.hotel = hotel;
    }
}