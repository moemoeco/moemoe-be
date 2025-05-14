package com.moemoe.redis.repository;

import com.moemoe.redis.config.EmbeddedRedisConfig;
import com.moemoe.redis.config.RedisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest(properties = "application.yml")
@ContextConfiguration(classes = {RedisConfig.class, EmbeddedRedisConfig.class})
class SubscriptionRepositoryTest {
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private final String roomId = "roomId";
    private final String userId = "userId";
    private final String subscribersKeyPrefix = "chat:subscribers:";
    private final String chatRoomsKeyPrefix = "chat:chatRooms:";

    @AfterEach
    void deleteAll() {
        stringRedisTemplate.delete(subscribersKeyPrefix + roomId);
        stringRedisTemplate.delete(chatRoomsKeyPrefix + userId);
    }

    @Test
    @DisplayName("성공 케이스 : 채팅방 구독자 추가")
    void addSubscriber() {
        // when
        subscriptionRepository.addSubscriber(roomId, userId);

        // then
        Set<String> subscribersMembers = stringRedisTemplate.opsForSet().members(subscribersKeyPrefix + roomId);
        assertThat(subscribersMembers)
                .hasSize(1)
                .containsExactly(userId);

        Set<String> chatRoomMembers = stringRedisTemplate.opsForSet().members(chatRoomsKeyPrefix + userId);
        assertThat(chatRoomMembers)
                .hasSize(1)
                .containsExactly(roomId);
    }

    @Test
    @DisplayName("성공 케이스 : 채팅방 구독자 제거")
    void removeSubscriber() {
        // given
        stringRedisTemplate.opsForSet()
                .add(subscribersKeyPrefix + roomId, userId);
        stringRedisTemplate.opsForSet()
                .add(chatRoomsKeyPrefix + userId, roomId);

        // when
        subscriptionRepository.removeSubscriber(roomId, userId);

        // then
        Set<String> subscribersMembers = stringRedisTemplate.opsForSet().members(subscribersKeyPrefix + roomId);
        assertThat(subscribersMembers)
                .isEmpty();

        Set<String> chatRoomMembers = stringRedisTemplate.opsForSet().members(chatRoomsKeyPrefix + userId);
        assertThat(chatRoomMembers)
                .isEmpty();
    }

    @Test
    @DisplayName("성공 케이스 : 채팅방 구독자 조회")
    void getSubscribers() {
        // given
        String user1 = "user1";
        String user2 = "user2";
        String user3 = "user3";
        stringRedisTemplate.opsForSet()
                .add(subscribersKeyPrefix + roomId, user1);
        stringRedisTemplate.opsForSet()
                .add(subscribersKeyPrefix + roomId, user2);
        stringRedisTemplate.opsForSet()
                .add(subscribersKeyPrefix + roomId, user3);

        // when
        Set<String> subscribers = subscriptionRepository.getSubscribers(roomId);

        // then
        assertThat(subscribers)
                .hasSize(3)
                .containsExactlyInAnyOrder(user1, user2, user3);
    }

    @Test
    @DisplayName("성공 케이스 : 유저가 구독 중인 채팅방 조회")
    void getChatRooms() {
        // given
        String room1 = "room1";
        String room2 = "room2";
        String room3 = "room3";
        stringRedisTemplate.opsForSet()
                .add(chatRoomsKeyPrefix + userId, room1);
        stringRedisTemplate.opsForSet()
                .add(chatRoomsKeyPrefix + userId, room2);
        stringRedisTemplate.opsForSet()
                .add(chatRoomsKeyPrefix + userId, room3);

        // when
        Set<String> chatRooms = subscriptionRepository.getChatRooms(userId);

        // then
        assertThat(chatRooms)
                .hasSize(3)
                .containsExactlyInAnyOrder(room1, room2, room3);
    }
}