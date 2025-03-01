package com.moemoe.redis.entity;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "refresh_token")
public class RefreshTokenEntity {
    @Id
    private String email;
    @Indexed
    private String token;
    @TimeToLive
    private long expirationInSeconds;
    private static final long DEFAULT_TTL = 2592000L;

    public static RefreshTokenEntity of(@NotEmpty String email,
                                        @NotEmpty String refreshToken) {
        return new RefreshTokenEntity(email, refreshToken, DEFAULT_TTL);
    }

    public static RefreshTokenEntity of(@NotEmpty String email,
                                        @NotEmpty String refreshToken,
                                        long expirationInSeconds) {
        return new RefreshTokenEntity(email, refreshToken, expirationInSeconds);
    }

    public RefreshTokenEntity(String email, String token, long expirationInSeconds) {
        this.email = email;
        this.token = token;
        this.expirationInSeconds = expirationInSeconds;
    }
}
