package com.moemoe.chat.listener;

import com.moemoe.redis.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Set;

import static org.mockito.BDDMockito.*;

@ExtendWith(SpringExtension.class)
class StompEventListenerTest {

    private StompEventListener listener;
    private SubscriptionRepository repository;
    private static final String VALID_DESTINATION_PREFIX = "/topic/chat.";
    private static final String INVALID_DESTINATION_PREFIX = "/topic/wrong.";
    private static final String SESSION_ID = "sessionId";

    @BeforeEach
    void setup() {
        repository = mock(SubscriptionRepository.class);
        listener = new StompEventListener(repository);
    }

    // given: STOMP 메시지 빌더
    private Message<byte[]> buildMessage(StompCommand cmd, String dest, String userId) {
        StompHeaderAccessor header = StompHeaderAccessor.create(cmd);
        header.setDestination(dest);
        if (userId != null) {
            header.setUser(() -> userId);
        }
        header.setSessionId(SESSION_ID);
        return MessageBuilder.createMessage(new byte[0], header.getMessageHeaders());
    }

    @Test
    @DisplayName("성공 케이스 : handleSubscribe: 올바른 토픽 구독 시 addSubscriber 호출")
    void subscribe_valid() {
        // given
        String roomId = "roomId";
        String userId = "userId";
        Message<byte[]> msg = buildMessage(
                StompCommand.SUBSCRIBE,
                VALID_DESTINATION_PREFIX + roomId,
                userId
        );

        // when
        listener.handleSubscribe(new SessionSubscribeEvent(this, msg));

        // then
        then(repository)
                .should()
                .addSubscriber(roomId, userId);
        then(repository)
                .shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("성공 케이스 : handleSubscribe: 잘못된 destination 이면 호출 무시")
    void subscribe_invalidDestination() {
        // given
        String roomId = "roomId";
        String userId = "userId";
        Message<byte[]> msg = buildMessage(
                StompCommand.SUBSCRIBE,
                INVALID_DESTINATION_PREFIX + roomId,
                userId
        );

        // when
        listener.handleSubscribe(new SessionSubscribeEvent(this, msg));

        // then
        then(repository)
                .shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("성공 케이스 : handleSubscribe: user 없으면 호출 무시")
    void subscribe_noUser() {
        // given
        String roomId = "roomId";
        Message<byte[]> msg = buildMessage(
                StompCommand.SUBSCRIBE,
                VALID_DESTINATION_PREFIX + roomId,
                null
        );

        // when
        listener.handleSubscribe(new SessionSubscribeEvent(this, msg));

        // then
        then(repository)
                .shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("성공 케이스 : handleUnsubscribe: 올바른 구독 해제 시 removeSubscriber 호출")
    void unsubscribe_valid() {
        // given
        String roomId = "roomId";
        String userId = "userId";
        Message<byte[]> msg = buildMessage(
                StompCommand.UNSUBSCRIBE,
                VALID_DESTINATION_PREFIX + roomId,
                userId
        );

        // when
        listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, msg));

        // then
        then(repository)
                .should()
                .removeSubscriber(roomId, userId);
        then(repository)
                .shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("성공 케이스 : handleUnsubscribe: 잘못된 destination 시 무시")
    void unsubscribe_invalidDestination() {
        // given
        String roomId = "roomId";
        String userId = "userId";
        Message<byte[]> msg = buildMessage(
                StompCommand.UNSUBSCRIBE,
                INVALID_DESTINATION_PREFIX + roomId,
                userId
        );

        // when
        listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, msg));

        // then
        then(repository)
                .shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("성공 케이스 : handleUnsubscribe: 유저가 없는 경우 무시")
    void unsubscribe_noUser() {
        // given
        String roomId = "roomId";
        Message<byte[]> msg = buildMessage(
                StompCommand.UNSUBSCRIBE,
                VALID_DESTINATION_PREFIX + roomId,
                null
        );

        // when
        listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, msg));

        // then
        then(repository)
                .shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("성공 케이스 : handleDisconnect: getChatRooms 결과로 removeSubscriber 반복 호출")
    void disconnect_valid() {
        // given
        String userId = "userId";
        String roomId1 = "roomId1";
        String roomId2 = "roomId2";
        Message<byte[]> msg = buildMessage(
                StompCommand.DISCONNECT,
                null,
                userId
        );
        given(repository.getChatRooms(userId))
                .willReturn(Set.of(roomId1, roomId2));

        // when
        SessionDisconnectEvent event = new SessionDisconnectEvent(
                this,
                msg,
                SESSION_ID,
                CloseStatus.NORMAL
        );
        listener.handleDisconnect(event);

        // then
        then(repository)
                .should()
                .getChatRooms(userId);
        then(repository)
                .should()
                .removeSubscriber(roomId1, userId);
        then(repository)
                .should()
                .removeSubscriber(roomId2, userId);
        then(repository)
                .shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("성공 케이스 : handleDisconnect: user 없으면 호출 무시")
    void disconnect_noUser() {
        // given
        Message<byte[]> msg = buildMessage(
                StompCommand.DISCONNECT,
                null,
                null
        );

        // when
        SessionDisconnectEvent event = new SessionDisconnectEvent(
                this,
                msg,
                SESSION_ID,
                CloseStatus.NORMAL
        );
        listener.handleDisconnect(event);

        // then
        then(repository)
                .shouldHaveNoInteractions();
    }
}
