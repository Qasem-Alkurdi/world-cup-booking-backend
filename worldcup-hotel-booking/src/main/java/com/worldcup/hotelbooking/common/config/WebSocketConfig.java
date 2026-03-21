package com.worldcup.hotelbooking.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration using STOMP over SockJS.
 *
 * Channel layout:
 *   /ws                           — SockJS handshake endpoint (frontend connects here)
 *   /topic/conversation/{id}      — broadcast topic per conversation
 *   /app                          — client-to-server prefix (reserved, not actively used)
 *
 * Why REST for sending, WebSocket only for receiving?
 *   REST endpoints carry @PreAuthorize — authorization stays in one place.
 *   WebSocket is purely for pushing saved messages to connected clients in real time.
 */
@Configuration
@EnableWebSocketMessageBroker// Enables WebSocket message handling, backed by a message broker. Using STOMP protocol over SockJS for fallback options.
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");// Enables a simple in-memory message broker for topics prefixed with "/topic". Clients subscribe to these topics to receive messages. websocket here using for pushing messages to clients not for sending messages from clients to service, so we don't need to configure application destination prefix.

    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {// Registers the "/ws" endpoint for WebSocket connections, allowing all origins and enabling SockJS fallback options for browsers that don't support native WebSockets.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}