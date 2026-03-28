package com.worldcup.hotelbooking.chat;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Chat", description = "Real-time messaging between guests and hotel managers")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GUEST ENDPOINTS  —  /hotels/{hotelId}/chat
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /hotels/{hotelId}/chat
     *
     * Guest opens the chat panel on a hotel page.
     * Returns the full conversation history (creates it if first time).
     * Marks unread messages from the manager as read.
     *
     * Only the authenticated guest can call this — they always see their own conversation.
     */
    @GetMapping("/hotels/{hotelId}/chat")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary  = "Open hotel chat (guest)",
            description = "Returns or creates the conversation between the authenticated guest and the hotel."
    )
    public ResponseEntity<ConversationResponse> openHotelChat(
            @PathVariable Long hotelId,
            Authentication authentication) {

        Long guestId = extractUserId(authentication);
        return ResponseEntity.ok(chatService.getOrCreateGuestConversation(hotelId, guestId));
    }

    /**
     * POST /hotels/{hotelId}/chat/messages
     *
     * Guest sends a message to a hotel.
     * Creates the conversation if this is the first message.
     * Also pushed in real time to the manager via WebSocket.
     */
    @PostMapping("/hotels/{hotelId}/chat/messages")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary  = "Send message to hotel (guest)",
            description = "Guest sends a text message. Pushed in real time via WebSocket to the hotel manager."
    )
    public ResponseEntity<ChatMessageResponse> guestSendMessage(
            @PathVariable Long hotelId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {

        Long guestId = extractUserId(authentication);
        return ResponseEntity.ok(chatService.guestSendMessage(hotelId, guestId, request.content()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MANAGER ENDPOINTS  —  /conversations/{conversationId}
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /conversations/{conversationId}
     *
     * Manager opens a specific conversation from their inbox.
     * Only the hotel owner of that conversation can access it.
     */
    @GetMapping("/conversations/{conversationId}")
    @PreAuthorize("""
            hasRole('ADMIN')
            or @bookingAuthorizationService.isConversationParticipant(#conversationId, authentication)
            """)
    @Operation(
            summary  = "Open a conversation (manager / admin)",
            description = "Manager views a specific guest conversation. Also accessible by the guest themselves."
    )
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable Long conversationId,
            Authentication authentication) {

        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(chatService.getConversationById(conversationId, userId));
    }

    /**
     * POST /conversations/{conversationId}/messages
     *
     * Manager replies in a guest conversation.
     * Also pushed in real time to the guest via WebSocket.
     */
    @PostMapping("/conversations/{conversationId}/messages")
    @PreAuthorize("""
            hasRole('ADMIN')
            or(hasRole('MANAGER') and @bookingAuthorizationService.isConversationParticipant(#conversationId, authentication))
            """)
    @Operation(
            summary  = "Reply in a conversation (manager)",
            description = "Manager sends a reply. Pushed in real time via WebSocket to the guest."
    )
    public ResponseEntity<ChatMessageResponse> managerSendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {

        Long managerId = extractUserId(authentication);
        return ResponseEntity.ok(
                chatService.managerSendMessage(conversationId, managerId, request.content()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPER — mirrors extractUserId in BookingAuthorizationService
    // ─────────────────────────────────────────────────────────────────────────

    private Long extractUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            Object claim = jwt.getClaim("userId");
            if (claim instanceof Integer i) return i.longValue();
            if (claim instanceof Long l)    return l;
            if (claim instanceof String s)  return Long.parseLong(s);
        }
        throw new IllegalStateException("Cannot extract userId from authentication");
    }
}