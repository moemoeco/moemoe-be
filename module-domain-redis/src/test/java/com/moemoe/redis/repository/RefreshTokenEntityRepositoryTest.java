package com.moemoe.redis.repository;

import com.moemoe.redis.config.EmbeddedRedisConfig;
import com.moemoe.redis.config.RedisConfig;
import com.moemoe.redis.entity.RefreshTokenEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@DataRedisTest(properties = "application.yml")
@ContextConfiguration(classes = {RedisConfig.class, EmbeddedRedisConfig.class})
class RefreshTokenEntityRepositoryTest {
    @Autowired
    private RefreshTokenEntityRepository refreshTokenEntityRepository;

    @AfterEach
    void destroy() {
        refreshTokenEntityRepository.deleteAll();
    }

    @Test
    @DisplayName("성공 케이스 : refresh token 생성, 조회")
    void create_select() {
        String expectedEmail = "test@example.com";
        String expectedRefreshToken = "expectedRefreshToken";

        refreshTokenEntityRepository.save(RefreshTokenEntity.of(expectedEmail, expectedRefreshToken));

        Optional<RefreshTokenEntity> actualRefreshToken = refreshTokenEntityRepository.findById(expectedEmail);
        assertThat(actualRefreshToken)
                .isNotEmpty();
        long defaultRefreshExpiration = 2592000L;
        RefreshTokenEntity refreshTokenEntity = actualRefreshToken.get();
        assertThat(refreshTokenEntity)
                .extracting(RefreshTokenEntity::getToken, RefreshTokenEntity::getEmail, RefreshTokenEntity::getExpirationInSeconds)
                .containsExactly(expectedRefreshToken, expectedEmail, defaultRefreshExpiration);
    }

    @Test
    @DisplayName("성공 케이스 : refresh token ttl")
    void ttl() {
        String expectedEmail = "test@example.com";
        String expectedRefreshToken = "expectedRefreshToken";
        long expectedRefreshExpiration = 1L;

        refreshTokenEntityRepository.save(RefreshTokenEntity.of(expectedEmail, expectedRefreshToken, expectedRefreshExpiration));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail();
        }
        Optional<RefreshTokenEntity> actualRefreshToken = refreshTokenEntityRepository.findById(expectedEmail);
        assertThat(actualRefreshToken)
                .isEmpty();
    }

    @Test
    @DisplayName("성공 케이스 : refresh token token 값으로 조회")
    void findByToken() {
        // given
        refreshTokenEntityRepository.deleteAll();
        String expectedEmail = "test@example.com";
        String expectedRefreshToken = "expectedRefreshToken2";
        refreshTokenEntityRepository.save(RefreshTokenEntity.of(expectedEmail, expectedRefreshToken));

        // when
        RefreshTokenEntity byToken = refreshTokenEntityRepository.findByToken(expectedRefreshToken)
                .orElseThrow();

        // then
        assertThat(byToken)
                .extracting(RefreshTokenEntity::getEmail, RefreshTokenEntity::getToken)
                .containsExactly(expectedEmail, expectedRefreshToken);
    }

    @Test
    @DisplayName("성공 케이스 : refresh token 삭제")
    void delete() {
        // given
        refreshTokenEntityRepository.deleteAll();
        String expectedEmail = "test@example.com";
        String expectedRefreshToken = "expectedRefreshToken2";
        RefreshTokenEntity save = refreshTokenEntityRepository.save(RefreshTokenEntity.of(expectedEmail, expectedRefreshToken));

        // when
        refreshTokenEntityRepository.delete(save);

        // then
        assertThat(refreshTokenEntityRepository.findByToken(expectedRefreshToken))
                .isEmpty();
    }
}