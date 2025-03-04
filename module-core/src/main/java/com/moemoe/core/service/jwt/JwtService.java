package com.moemoe.core.service.jwt;

import com.moemoe.core.service.jwt.exception.JwtExpiredException;
import com.moemoe.core.service.jwt.exception.JwtMalformedException;
import com.moemoe.mongo.constant.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    public static final String AUTHENTICATION_HEADER = "Authorization";

    @Value("${service.jwt.issuer}")
    private String issuer;
    @Value("${service.jwt.access-expiration}")
    private long accessExpiration;
    @Value("${service.jwt.refresh-expiration}")
    private long refreshExpiration;
    private final SecretKey secretKey;

    public JwtService(@Value("${service.jwt.secret-key}") String secretKey) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
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

    public String getUserId(String token) {
        return extractClaims(token)
                .getSubject();
    }

    public String getEmail(String token) {
        return extractClaims(token)
                .get("email", String.class);
    }

    public String getRole(String token) {
        return extractClaims(token)
                .get("role", String.class);
    }

    public boolean isValidToken(String token, String userId) {
        try {
            Claims claims = extractClaims(token);
            if (!claims.containsKey("role")) {
                UserRole.valueOf(claims.get("role", String.class));
                return false;
            }
            if (!claims.containsKey("email")) {
                return false;
            }

            String subject = claims.getSubject();
            return userId.equals(subject) && !isExpiredToken(token);
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
