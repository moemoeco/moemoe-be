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
    private static final String KEY_PREFIX = "chat:subscribers:";

    @Override
    public void addSubscriber(String roomId, String userId) {
        stringRedisTemplate.opsForSet().add(getKey(roomId), userId);
    }

    @Override
    public void removeSubscriber(String roomId, String userId) {
        stringRedisTemplate.opsForSet().remove(getKey(roomId), userId);
    }

    @Override
    public Set<String> getSubscribers(String roomId) {
        Set<String> members = stringRedisTemplate.opsForSet()
                .members(getKey(roomId));
        return (members != null) ? members : Collections.emptySet();
    }

    private String getKey(String roomId) {
        return KEY_PREFIX + roomId;
    }
}
