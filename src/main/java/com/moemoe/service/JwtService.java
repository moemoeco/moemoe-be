package com.moemoe.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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

    public String getEmail(String token) {
        return extractClaims(token)
                .getSubject();
    }

    public boolean isExpired(String token) {
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
