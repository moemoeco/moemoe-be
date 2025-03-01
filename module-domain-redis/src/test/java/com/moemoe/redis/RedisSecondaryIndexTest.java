package com.moemoe.redis;

import com.moemoe.redis.config.EmbeddedRedisConfig;
import com.moemoe.redis.config.RedisConfig;
import com.moemoe.redis.entity.RefreshTokenEntity;
import com.moemoe.redis.repository.RefreshTokenEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DataRedisTest(properties = "application.yml")
@ContextConfiguration(classes = {RedisConfig.class, EmbeddedRedisConfig.class})
class RedisSecondaryIndexTest {
    @Autowired
    private RefreshTokenEntityRepository refreshTokenEntityRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("성공 케이스 : TTL 만료 이후 Secondary Index 삭제")
    void onMessage() {
        // given
        String secondaryIndex = "refresh_token:token:refreshtoken";
        String tokenIndexKey = "refresh_token:test@example.com:idx";
        refreshTokenEntityRepository.save(RefreshTokenEntity.of("test@example.com", "refreshtoken", 3L));
        assertThat(redisTemplate.opsForSet().isMember(tokenIndexKey, secondaryIndex))
                .isTrue();

        //when
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> Boolean.FALSE.equals(redisTemplate.opsForSet().isMember(tokenIndexKey, secondaryIndex)));

        assertThat(redisTemplate.opsForSet().isMember(tokenIndexKey, secondaryIndex))
                .isFalse();
    }
}