package com.worldcup.hotelbooking.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /hotels/{hotelId}/chat/messages
 */
public record SendMessageRequest(

        @NotBlank(message = "Message content cannot be blank")
        @Size(max = 2000, message = "Message cannot exceed 2000 characters")
        String content
) {}