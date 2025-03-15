package com.moemoe.core.service;

import com.moemoe.core.service.jwt.JwtService;
import com.moemoe.core.service.jwt.exception.JwtExpiredException;
import com.moemoe.mongo.constant.UserRole;
import com.moemoe.mongo.entity.UserEntity;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;

import static com.nimbusds.jose.JWSAlgorithm.HS256;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


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

    @Test
    @DisplayName("성공 케이스 : Access Token 생성")
    void createAccessToken() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // given
        ObjectId id = new ObjectId();
        String email = "test@example.com";
        UserRole role = UserRole.USER;

        UserEntity userEntity = getUserEntity(id, role, email);

        // when
        Map<String, String> claims = Map.of("email", email, "role", role.name());
        String accessToken = jwtService.createAccessToken(claims, userEntity.getId());

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
                .isEqualTo(id.toHexString());
        assertThat(payload.getIssuer())
                .isEqualTo("test-issuer");
        assertThat(payload.getExpiration().getTime() - payload.getIssuedAt().getTime())
                .isEqualTo(accessExpiration);
        assertThat(payload.get("email", String.class))
                .isEqualTo(email);
        assertThat(payload.get("role", String.class))
                .isEqualTo(role.name());
    }


    @Test
    @DisplayName("성공 케이스 : Refresh Token 생성")
    void createRefreshToken() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // given
        ObjectId id = new ObjectId();
        String email = "test@example.com";
        UserRole role = UserRole.USER;

        UserEntity userEntity = getUserEntity(id, role, email);

        // when
        Map<String, String> claims = Map.of("email", email, "role", role.name());
        String refreshToken = jwtService.createRefreshToken(claims, userEntity.getId());

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
                .isEqualTo(id.toHexString());
        assertThat(payload.getIssuer())
                .isEqualTo("test-issuer");
        assertThat(payload.getExpiration().getTime() - payload.getIssuedAt().getTime())
                .isEqualTo(refreshExpiration);
        assertThat(payload.get("email", String.class))
                .isEqualTo(email);
        assertThat(payload.get("role", String.class))
                .isEqualTo(role.name());
    }

    @Test
    @DisplayName("성공 케이스 : 유효한 토큰")
    void isValid() {
        // given
        ObjectId id = new ObjectId();
        String email = "test@example.com";
        UserRole role = UserRole.USER;
        long now = System.currentTimeMillis();
        Map<String, String> claims = Map.of("email", email, "role", role.name());
        String jwtToken = Jwts.builder()
                .header()
                .type("jwt")
                .and()
                .claims(claims)
                .subject(id.toHexString())
                .expiration(new Date(now + accessExpiration))
                .issuedAt(new Date(now))
                .issuer("")
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey)))
                .compact();

        // when then
        assertTrue(jwtService.isValidToken(jwtToken, id.toHexString()));
        assertFalse(jwtService.isValidToken(jwtToken, new ObjectId().toHexString()));
    }

    @Test
    @DisplayName("실패 케이스 : 유효한 토큰 검증 시 유효 시간이 다 된 경우")
    void isValidTokenExpiration() {
        // given
        ObjectId id = new ObjectId();
        String email = "test@example.com";
        UserRole role = UserRole.USER;
        long now = System.currentTimeMillis();
        Map<String, String> claims = Map.of("email", email, "role", role.name());
        String jwtToken = Jwts.builder()
                .header()
                .type("jwt")
                .and()
                .claims(claims)
                .subject(id.toHexString())
                .expiration(new Date(now))
                .issuedAt(new Date(now))
                .issuer("")
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey)))
                .compact();

        // when
        assertThatThrownBy(() -> jwtService.isValidToken(jwtToken, id.toHexString()))
                .isInstanceOf(JwtExpiredException.class);
    }

    private UserEntity getUserEntity(ObjectId id, UserRole role, String email) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<UserEntity> constructor = UserEntity.class.getDeclaredConstructor();
        constructor.setAccessible(true);  // private 생성자 접근 허용
        UserEntity userEntity = constructor.newInstance();
        ReflectionTestUtils.setField(userEntity, "id", id);
        ReflectionTestUtils.setField(userEntity, "role", role);
        ReflectionTestUtils.setField(userEntity, "email", email);
        return userEntity;
    }
}