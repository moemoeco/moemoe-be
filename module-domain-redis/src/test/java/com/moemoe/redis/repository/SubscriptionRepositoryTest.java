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
    private final String keyPrefix = "chat:subscribers:";

    @AfterEach
    void deleteAll() {
        stringRedisTemplate.delete(keyPrefix + roomId);
    }

    @Test
    @DisplayName("성공 케이스 : 채팅방 구독자 추가")
    void addSubscriber() {
        // when
        subscriptionRepository.addSubscriber(roomId, userId);

        // then
        Set<String> members = stringRedisTemplate.opsForSet().members(keyPrefix + roomId);
        assertThat(members)
                .hasSize(1)
                .containsExactly(userId);
    }

    @Test
    @DisplayName("성공 케이스 : 채팅방 구독자 제거")
    void removeSubscriber() {
        // given
        stringRedisTemplate.opsForSet()
                .add(keyPrefix + roomId, userId);

        // when
        subscriptionRepository.removeSubscriber(roomId, userId);

        // then
        Set<String> members = stringRedisTemplate.opsForSet().members(keyPrefix + roomId);
        assertThat(members)
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
                .add(keyPrefix + roomId, user1);
        stringRedisTemplate.opsForSet()
                .add(keyPrefix + roomId, user2);
        stringRedisTemplate.opsForSet()
                .add(keyPrefix + roomId, user3);


        // when
        Set<String> subscribers = subscriptionRepository.getSubscribers(roomId);

        // then
        assertThat(subscribers)
                .hasSize(3)
                .containsExactlyInAnyOrder(user1, user2, user3);
    }
}