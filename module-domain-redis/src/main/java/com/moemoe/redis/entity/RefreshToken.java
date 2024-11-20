package com.moemoe.redis.entity;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "refresh_token")
public class RefreshToken {
    @Id
    private String email;
    private String token;
    @TimeToLive
    private long expirationInSeconds;
    private static final long DEFAULT_TTL = 2592000L;

    public static RefreshToken of(@NotEmpty String email,
                                  @NotEmpty String refreshToken) {
        return new RefreshToken(email, refreshToken, DEFAULT_TTL);
    }

    public static RefreshToken of(@NotEmpty String email,
                                  @NotEmpty String refreshToken,
                                  long expirationInSeconds) {
        return new RefreshToken(email, refreshToken, expirationInSeconds);
    }

    public RefreshToken(String email, String token, long expirationInSeconds) {
        this.email = email;
        this.token = token;
        this.expirationInSeconds = expirationInSeconds;
    }
}
