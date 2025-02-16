package com.moemoe.core.service;

import com.moemoe.core.request.LogoutRequest;
import com.moemoe.core.request.RefreshAccessTokenRequest;
import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.jwt.ClaimsFactory;
import com.moemoe.core.service.jwt.JwtService;
import com.moemoe.mongo.entity.User;
import com.moemoe.mongo.repository.UserEntityRepository;
import com.moemoe.redis.entity.RefreshToken;
import com.moemoe.redis.repository.RefreshTokenEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final RefreshTokenEntityRepository refreshTokenEntityRepository;
    private final UserEntityRepository userEntityRepository;
    private final JwtService jwtService;

    public LoginTokenResponse refresh(RefreshAccessTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        RefreshToken refreshTokenEntity = refreshTokenEntityRepository.findByToken(refreshToken)
                .orElseThrow();
        User userEntity = userEntityRepository.findByEmail(refreshTokenEntity.getEmail())
                .orElseThrow();

        Map<String, String> userClaims = ClaimsFactory.getUserClaims(userEntity);
        final String accessToken = jwtService.createAccessToken(userClaims, userEntity);

        return LoginTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String refreshToken = request.getRefreshToken();
        refreshTokenEntityRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenEntityRepository::delete);
        log.info("User logged out.");
    }

    public User getUser(String email) {
        return userEntityRepository.findByEmail(email)
                .orElseThrow();
    }
}

