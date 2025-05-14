package com.moemoe.redis.repository;

import java.util.Set;

public interface SubscriptionRepository {
    void addSubscriber(String roomId, String userId);

    void removeSubscriber(String roomId, String userId);

    Set<String> getSubscribers(String roomId);
}
