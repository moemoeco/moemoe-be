package com.moemoe.chat.listener;

import com.moemoe.redis.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.security.Principal;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventListener {
    private static final String CHAT_TOPIC_PREFIX = "/topic/chat.";
    private final SubscriptionRepository subscriptionRepository;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor header = StompHeaderAccessor.wrap(event.getMessage());
        String destination = header.getDestination();
        String roomId = getRoomId(destination);
        Principal user = header.getUser();

        if (roomId != null && user != null) {
            String userId = user.getName();
            log.info("User '{}' is subscribing to room '{}'", userId, roomId);
            subscriptionRepository.addSubscriber(roomId, userId);
        } else {
            log.warn("Ignored subscribe: invalid destination='{}' or missing user='{}'", destination, user);
        }
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor header = StompHeaderAccessor.wrap(event.getMessage());
        String destination = header.getDestination();
        String roomId = getRoomId(destination);
        if (roomId == null) {
            roomId = header.getSubscriptionId();
        }
        Principal user = header.getUser();

        if (roomId != null && user != null) {
            String userId = user.getName();
            log.info("User '{}' is unsubscribing from room '{}'", userId, roomId);
            subscriptionRepository.removeSubscriber(roomId, userId);
        } else {
            log.warn("Ignored unsubscribe: destination='{}', subscriptionId='{}', user='{}'",
                    destination, header.getSubscriptionId(), user);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor header = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = header.getUser();

        if (user != null) {
            String userId = user.getName();
            Set<String> rooms = subscriptionRepository.getChatRooms(userId);
            log.info("User '{}' disconnected; cleaning up subscriptions for rooms {}", userId, rooms);
            rooms.forEach(roomId -> {
                log.debug("Removing '{}' from room '{}'", userId, roomId);
                subscriptionRepository.removeSubscriber(roomId, userId);
            });
        } else {
            log.warn("Ignored disconnect: no user principal found in header");
        }
    }

    private String getRoomId(String destination) {
        if (destination != null && destination.startsWith(CHAT_TOPIC_PREFIX)) {
            return destination.substring(CHAT_TOPIC_PREFIX.length());
        }
        return null;
    }
}
