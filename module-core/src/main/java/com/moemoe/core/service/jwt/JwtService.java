package com.moemoe.core.service.jwt;

import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.jwt.exception.JwtExpiredException;
import com.moemoe.core.service.jwt.exception.JwtMalformedException;
import com.moemoe.mongo.constant.UserRole;
import com.moemoe.mongo.entity.User;
import com.moemoe.mongo.repository.UserEntityRepository;
import com.moemoe.redis.entity.RefreshToken;
import com.moemoe.redis.repository.RefreshTokenEntityRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final RefreshTokenEntityRepository refreshTokenEntityRepository;
    private final UserEntityRepository userEntityRepository;

    @Value("${service.jwt.issuer}")
    private String issuer;
    @Value("${service.jwt.access-expiration}")
    private long accessExpiration;
    @Value("${service.jwt.refresh-expiration}")
    private long refreshExpiration;
    private final SecretKey secretKey;

    public JwtService(RefreshTokenEntityRepository refreshTokenEntityRepository,
                      UserEntityRepository userEntityRepository,
                      @Value("${service.jwt.secret-key}") String secretKey) {
        this.refreshTokenEntityRepository = refreshTokenEntityRepository;
        this.userEntityRepository = userEntityRepository;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
    }

    @Transactional(readOnly = true)
    public LoginTokenResponse refresh(String refreshToken) {
        RefreshToken refreshTokenEntity = refreshTokenEntityRepository.findByToken(refreshToken)
                .orElseThrow();
        User userEntity = userEntityRepository.findByEmail(refreshTokenEntity.getEmail())
                .orElseThrow();

        Map<String, String> userClaims = ClaimsFactory.getUserClaims(userEntity);
        final String accessToken = createAccessToken(userClaims, userEntity);

        return LoginTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public String createAccessToken(Map<String, String> claims, UserDetails userDetails) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .header()
                .type("jwt")
                .and()
                .claims(claims)
                .subject(userDetails.getUsername())
                .expiration(new Date(now + accessExpiration))
                .issuedAt(new Date(now))
                .issuer(issuer)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Map<String, String> claims, UserDetails userDetails) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .header()
                .type("jwt")
                .and()
                .claims(claims)
                .subject(userDetails.getUsername())
                .expiration(new Date(now + refreshExpiration))
                .issuedAt(new Date(now))
                .issuer(issuer)
                .signWith(secretKey)
                .compact();
    }

    public String getEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    public String getRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public boolean isValidToken(String token, String userName) {
        try {
            Claims claims = extractClaims(token);
            if (!claims.containsKey("role")) {
                UserRole.valueOf(claims.get("role", String.class));
                return false;
            }
            if (!claims.containsKey("email")) return false;

            String subject = claims.getSubject();
            return userName.equals(subject) && !isExpiredToken(token);
        } catch (MalformedJwtException e) {
            throw new JwtMalformedException(e.getMessage(), e);
        } catch (ExpiredJwtException e) {
            throw new JwtExpiredException(e.getMessage(), e);
        }
    }

    private boolean isExpiredToken(String token) {
        Date expiration = extractClaims(token)
                .getExpiration();
        return expiration.before(new Date());
    }

    private Claims extractClaims(String token) {
        return (Claims) Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parse(token)
                .getPayload();
    }
}
