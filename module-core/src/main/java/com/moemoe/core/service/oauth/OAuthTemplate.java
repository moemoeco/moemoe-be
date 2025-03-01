package com.moemoe.core.service.oauth;

import com.moemoe.client.http.dto.TokenResponse;
import com.moemoe.client.http.dto.UserInfoResponse;
import com.moemoe.core.response.AuthorizationResponse;
import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.jwt.ClaimsFactory;
import com.moemoe.core.service.jwt.JwtService;
import com.moemoe.mongo.entity.UserEntity;
import com.moemoe.mongo.repository.UserEntityRepository;
import com.moemoe.redis.entity.RefreshTokenEntity;
import com.moemoe.redis.repository.RefreshTokenEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

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
        UserEntity userEntity = getUserEntity(userInfo);

        Map<String, String> userClaims = ClaimsFactory.getUserClaims(userEntity);
        final String accessToken = jwtService.createAccessToken(userClaims, userEntity);
        final String refreshToken = jwtService.createRefreshToken(userClaims, userEntity);

        refreshTokenEntityRepository.save(RefreshTokenEntity.of(userEntity.getEmail(), refreshToken));
        log.info("OAuth login service done.");
        return LoginTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public abstract AuthorizationResponse authorize(String state);

    protected abstract TokenResponse getToken(String code, String state);

    protected abstract UserInfoResponse getUserInfo(TokenResponse token);

    protected abstract UserEntity getUserEntity(UserInfoResponse userInfo);
}
