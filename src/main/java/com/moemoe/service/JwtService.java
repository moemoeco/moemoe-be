package com.moemoe.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

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

    public String createAccessToken(String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .header()
                .type("jwt")
                .and()
                .claims()
                .subject(email)
                .expiration(new Date(now + accessExpiration))
                .issuedAt(new Date(now))
                .issuer(issuer)
                .and()
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(String email) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .header()
                .type("jwt")
                .and()
                .claims()
                .subject(email)
                .expiration(new Date(now + refreshExpiration))
                .issuedAt(new Date(now))
                .issuer(issuer)
                .and()
                .signWith(secretKey)
                .compact();
    }
}
