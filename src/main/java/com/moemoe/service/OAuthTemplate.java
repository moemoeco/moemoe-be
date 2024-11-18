package com.moemoe.service;

import com.moemoe.domain.RefreshToken;
import com.moemoe.domain.User;
import com.moemoe.dto.LoginTokenResponse;
import com.moemoe.dto.TokenResponse;
import com.moemoe.dto.UserInfoResponse;
import com.moemoe.repository.RefreshTokenEntityRepository;
import com.moemoe.repository.UserEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;


@Slf4j
public abstract class OAuthTemplate {
    protected final UserEntityRepository userEntityRepository;
    private final RefreshTokenEntityRepository refreshTokenEntityRepository;
    private final JwtService jwtService;

    protected OAuthTemplate(UserEntityRepository userEntityRepository,
                            RefreshTokenEntityRepository refreshTokenEntityRepository,
                            JwtService jwtService) {
        this.userEntityRepository = userEntityRepository;
        this.refreshTokenEntityRepository = refreshTokenEntityRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginTokenResponse login(String code, String state) {
        log.info("OAuth login service started.");
        TokenResponse token = getToken(code, state);
        UserInfoResponse userInfo = getUserInfo(token);
        User userEntity = getUserEntity(userInfo);

        Map<String, String> claims = new HashMap<>();
        claims.put("email", userEntity.getEmail());
        claims.put("role", userEntity.getRole().name());

        final String accessToken = jwtService.createAccessToken(claims, userEntity);
        final String refreshToken = jwtService.createRefreshToken(claims, userEntity);

        refreshTokenEntityRepository.save(RefreshToken.of(userEntity.getEmail(), refreshToken));
        log.info("OAuth login service done.");
        return LoginTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public abstract String authorize(String state);

    protected abstract TokenResponse getToken(String code, String state);

    protected abstract UserInfoResponse getUserInfo(TokenResponse token);

    protected abstract User getUserEntity(UserInfoResponse userInfo);
}
