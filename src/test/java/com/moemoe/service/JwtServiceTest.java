package com.moemoe.service;

import com.moemoe.domain.User;
import com.moemoe.domain.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.crypto.SecretKey;
import java.util.Map;

import static com.nimbusds.jose.JWSAlgorithm.HS256;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@TestPropertySource(properties = {
        "service.jwt.issuer=test-issuer",
        "service.jwt.access-expiration=#{30 * 60 * 1000}", // 30분
        "service.jwt.refresh-expiration=#{30 * 60 * 60 * 1000}", // 30일
        "service.jwt.secret-key=3v6Gc1yQ_wQD9pbV1vCj3E2qE8-RLdljM2xU6yWg7ZI"
})
class JwtServiceTest {
    @Autowired
    private JwtService jwtService;
    @Value("${service.jwt.secret-key}")
    private String secretKey;
    @Value("${service.jwt.access-expiration}")
    private long accessExpiration;
    @Value("${service.jwt.refresh-expiration}")
    private long refreshExpiration;

    @Test
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
}