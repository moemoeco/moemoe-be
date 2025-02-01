package com.moemoe.core.service;

import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.jwt.JwtService;
import com.moemoe.core.service.jwt.exception.JwtExpiredException;
import com.moemoe.mongo.constant.UserRole;
import com.moemoe.mongo.entity.User;
import com.moemoe.mongo.repository.UserEntityRepository;
import com.moemoe.redis.entity.RefreshToken;
import com.moemoe.redis.repository.RefreshTokenEntityRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ReflectionUtils;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.nimbusds.jose.JWSAlgorithm.HS256;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {JwtService.class})
@TestPropertySource(properties = {
        "service.jwt.issuer=test-issuer",
        "service.jwt.access-expiration=#{30 * 60 * 1000}", // 30분
        "service.jwt.refresh-expiration=#{30 * 60 * 60 * 1000}", // 30일
        "service.jwt.secret-key=3v6Gc1yQ_wQD9pbV1vCj3E2qE8-RLdljM2xU6yWg7ZI"
})
class JwtServiceTest {
    @SpyBean
    private JwtService jwtService;
    @Value("${service.jwt.secret-key}")
    private String secretKey;
    @Value("${service.jwt.access-expiration}")
    private long accessExpiration;
    @Value("${service.jwt.refresh-expiration}")
    private long refreshExpiration;
    @MockBean
    private UserEntityRepository userEntityRepository;
    @MockBean
    private RefreshTokenEntityRepository refreshTokenEntityRepository;

    @Test
    @DisplayName("성공 케이스 : Access Token 재발행")
    void refresh() {
        // given
        String expectedRefreshToken = "refreshToken";
        String expectedEmail = "user@moemoe.com";
        RefreshToken refreshTokenEntity = RefreshToken.of(expectedEmail, expectedRefreshToken);
        User userEntity = User.builder()
                .email(expectedEmail)
                .role(UserRole.USER)
                .build();
        given(refreshTokenEntityRepository.findByToken(expectedRefreshToken))
                .willReturn(Optional.of(refreshTokenEntity));
        given(userEntityRepository.findByEmail(expectedEmail))
                .willReturn(Optional.of(userEntity));

        // when
        LoginTokenResponse loginTokenResponse = jwtService.refresh(expectedRefreshToken);

        // then
        assertThat(loginTokenResponse)
                .extracting(LoginTokenResponse::refreshToken)
                .isEqualTo(expectedRefreshToken);
        assertDoesNotThrow(() -> Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey)))
                .build()
                .parse(loginTokenResponse.accessToken())
                .getPayload());
        verify(refreshTokenEntityRepository, times(1))
                .findByToken(expectedRefreshToken);
        verify(userEntityRepository, times(1))
                .findByEmail(expectedEmail);
    }

    @Test
    @DisplayName("실패 케이스 : Refresh Token Entity 데이터가 없는 경우")
    void refreshNoRefreshTokenEntity() {
        // given
        String expectedRefreshToken = "refreshToken";
        given(refreshTokenEntityRepository.findByToken(expectedRefreshToken))
                .willReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> jwtService.refresh(expectedRefreshToken))
                .isInstanceOf(NoSuchElementException.class);

        // then
        verify(refreshTokenEntityRepository, times(1))
                .findByToken(expectedRefreshToken);
        verify(userEntityRepository, times(0))
                .findByEmail(any());
    }

    @Test
    @DisplayName("실패 케이스 : User Entity 데이터가 없는 경우")
    void refreshNoUserEntity() {
        // given
        String expectedRefreshToken = "refreshToken";
        String expectedEmail = "user@moemoe.com";
        RefreshToken refreshTokenEntity = RefreshToken.of(expectedEmail, expectedRefreshToken);
        given(refreshTokenEntityRepository.findByToken(expectedRefreshToken))
                .willReturn(Optional.of(refreshTokenEntity));
        given(userEntityRepository.findByEmail(expectedEmail))
                .willReturn(Optional.empty());

        // when
        assertThatThrownBy(() -> jwtService.refresh(expectedRefreshToken))
                .isInstanceOf(NoSuchElementException.class);

        // then
        verify(refreshTokenEntityRepository, times(1))
                .findByToken(expectedRefreshToken);
        verify(userEntityRepository, times(1))
                .findByEmail(expectedEmail);
    }

    @Test
    @DisplayName("성공 케이스 : Access Token 생성")
    void createAccessToken() {
        // given
        String email = "test@example.com";
        String role = UserRole.USER.name();
        Map<String, String> claims = Map.of("email", email, "role", role);
        User user = User.builder().email(email).build();

        // when
        String accessToken = jwtService.createAccessToken(claims, user);

        // then
        assertThat(accessToken)
                .isNotBlank();
        SecretKey secretKey1 = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
        JwtParser jwtParser = Jwts.parser().verifyWith(secretKey1).build();
        Jwt<?, ?> parsedAccessToken = jwtParser.parse(accessToken);

        Header header = parsedAccessToken.getHeader();
        assertThat(header.getAlgorithm())
                .isEqualTo(HS256.toString());
        assertThat(header.getType())
                .isEqualTo("jwt");

        Claims payload = (Claims) parsedAccessToken.getPayload();
        assertThat(payload.getSubject())
                .isEqualTo(email);
        assertThat(payload.getIssuer())
                .isEqualTo("test-issuer");
        assertThat(payload.getExpiration().getTime() - payload.getIssuedAt().getTime())
                .isEqualTo(accessExpiration);
        assertThat(payload.get("email", String.class))
                .isEqualTo(email);
        assertThat(payload.get("role", String.class))
                .isEqualTo(role);
    }

    @Test
    @DisplayName("성공 케이스 : Refresh Token 생성")
    void createRefreshToken() {
        // given
        String email = "test@example.com";
        String role = UserRole.USER.name();
        Map<String, String> claims = Map.of("email", email, "role", role);
        User user = User.builder().email(email).build();

        // when
        String refreshToken = jwtService.createRefreshToken(claims, user);

        // then
        assertThat(refreshToken)
                .isNotBlank();
        SecretKey secretKey1 = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
        JwtParser jwtParser = Jwts.parser().verifyWith(secretKey1).build();
        Jwt<?, ?> parsedRefreshToken = jwtParser.parse(refreshToken);

        Header header = parsedRefreshToken.getHeader();
        assertThat(header.getAlgorithm())
                .isEqualTo(HS256.toString());
        assertThat(header.getType())
                .isEqualTo("jwt");

        Claims payload = (Claims) parsedRefreshToken.getPayload();
        assertThat(payload.getSubject())
                .isEqualTo(email);
        assertThat(payload.getIssuer())
                .isEqualTo("test-issuer");
        assertThat(payload.getExpiration().getTime() - payload.getIssuedAt().getTime())
                .isEqualTo(refreshExpiration);
        assertThat(payload.get("email", String.class))
                .isEqualTo(email);
        assertThat(payload.get("role", String.class))
                .isEqualTo(role);
    }

    @Test
    @DisplayName("성공 케이스 : 유효한 토큰 검증")
    void isValidToken() throws IllegalAccessException {
        // given
        String email = "test@example.com";
        String role = UserRole.USER.name();
        Map<String, String> claims = Map.of("email", email, "role", role);
        com.moemoe.mongo.entity.User user = User.builder().email(email).build();

        Field accessExpirationField = ReflectionUtils.findField(jwtService.getClass(), "accessExpiration");
        assert accessExpirationField != null;
        accessExpirationField.setAccessible(true);
        long beforeFieldValue = accessExpirationField.getLong(jwtService);
        ReflectionUtils.setField(accessExpirationField, jwtService, 0L);

        // when
        String accessToken = jwtService.createAccessToken(claims, user);
        assertThatThrownBy(() -> jwtService.isValidToken(accessToken, email))
                .isInstanceOf(JwtExpiredException.class);

        ReflectionUtils.setField(accessExpirationField, jwtService, beforeFieldValue);
        accessExpirationField.setAccessible(false);
    }
}