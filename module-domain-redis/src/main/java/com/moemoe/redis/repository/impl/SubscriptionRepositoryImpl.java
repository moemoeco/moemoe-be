package com.moemoe.redis.repository.impl;

import com.moemoe.redis.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class SubscriptionRepositoryImpl implements SubscriptionRepository {
    private final StringRedisTemplate stringRedisTemplate;
    private static final String SUBSCRIBERS_KEY_PREFIX = "chat:subscribers:";
    private static final String CHATROOM_KEY_PREFIX = "chat:chatRooms:";

    @Override
    public void addSubscriber(String roomId, String userId) {
        stringRedisTemplate.opsForSet().add(getSubscribersKey(roomId), userId);
        stringRedisTemplate.opsForSet().add(getChatroomKeyPrefix(userId), roomId);
    }

    @Override
    public void removeSubscriber(String roomId, String userId) {
        stringRedisTemplate.opsForSet().remove(getSubscribersKey(roomId), userId);
        stringRedisTemplate.opsForSet().remove(getChatroomKeyPrefix(userId), roomId);
    }

    @Override
    public Set<String> getSubscribers(String roomId) {
        Set<String> members = stringRedisTemplate.opsForSet()
                .members(getSubscribersKey(roomId));
        return (members != null) ? members : Collections.emptySet();
    }

    @Override
    public Set<String> getChatRooms(String userId) {
        Set<String> members = stringRedisTemplate.opsForSet()
                .members(getChatroomKeyPrefix(userId));
        return (members != null) ? members : Collections.emptySet();
    }

    private String getSubscribersKey(String roomId) {
        return SUBSCRIBERS_KEY_PREFIX + roomId;
    }

    private String getChatroomKeyPrefix(String userId) {
        return CHATROOM_KEY_PREFIX + userId;
    }
}
