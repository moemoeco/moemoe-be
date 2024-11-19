package com.moemoe.repository;

import com.moemoe.domain.redis.RefreshToken;
import com.moemoe.repository.redis.RefreshTokenEntityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenEntityRepositoryTest {
    @Autowired
    private RefreshTokenEntityRepository refreshTokenEntityRepository;

    @Test
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
}