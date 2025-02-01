package com.moemoe.redis.repository;

import com.moemoe.redis.config.EmbeddedRedisConfig;
import com.moemoe.redis.config.RedisConfig;
import com.moemoe.redis.entity.RefreshToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@DataRedisTest
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
    void crud() {
        String expectedEmail = "test@example.com";
        String expectedRefreshToken = "expectedRefreshToken";

        refreshTokenEntityRepository.save(RefreshToken.of(expectedEmail, expectedRefreshToken));

        Optional<RefreshToken> actualRefreshToken = refreshTokenEntityRepository.findById(expectedEmail);
        assertThat(actualRefreshToken)
                .isNotEmpty();
        long defaultRefreshExpiration = 2592000L;
        RefreshToken refreshToken = actualRefreshToken.get();
        assertThat(refreshToken)
                .extracting(RefreshToken::getToken, RefreshToken::getEmail, RefreshToken::getExpirationInSeconds)
                .containsExactly(expectedRefreshToken, expectedEmail, defaultRefreshExpiration);
    }

    @Test
    @DisplayName("성공 케이스 : refresh token ttl")
    void ttl() {
        String expectedEmail = "test@example.com";
        String expectedRefreshToken = "expectedRefreshToken";
        long expectedRefreshExpiration = 1L;

        refreshTokenEntityRepository.save(RefreshToken.of(expectedEmail, expectedRefreshToken, expectedRefreshExpiration));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail();
        }
        Optional<RefreshToken> actualRefreshToken = refreshTokenEntityRepository.findById(expectedEmail);
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
        refreshTokenEntityRepository.save(RefreshToken.of(expectedEmail, expectedRefreshToken));

        // when
        RefreshToken byToken = refreshTokenEntityRepository.findByToken(expectedRefreshToken)
                .orElseThrow();

        // then
        assertThat(byToken)
                .extracting(RefreshToken::getEmail, RefreshToken::getToken)
                .containsExactly(expectedEmail, expectedRefreshToken);
    }
}