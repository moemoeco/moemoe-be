package com.moemoe.core.service.oauth;

import com.moemoe.client.http.dto.TokenResponse;
import com.moemoe.client.http.dto.UserInfoResponse;
import com.moemoe.client.http.dto.naver.NaverUserInfoResponse;
import com.moemoe.client.http.naver.NaverTokenClient;
import com.moemoe.client.http.naver.NaverUserInfoClient;
import com.moemoe.core.request.OAuthLoginRequest;
import com.moemoe.core.service.builder.NaverUrlBuilder;
import com.moemoe.core.service.builder.UrlBuilder;
import com.moemoe.core.service.jwt.JwtService;
import com.moemoe.mongo.constant.UserRole;
import com.moemoe.mongo.entity.UserEntity;
import com.moemoe.mongo.repository.UserEntityRepository;
import com.moemoe.redis.repository.RefreshTokenEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;

@Slf4j
@Service
public class NaverOAuthService extends OAuthTemplate {
    private final UrlBuilder naverUrlBuilder;
    private final NaverTokenClient naverTokenClient;
    private final NaverUserInfoClient naverUserInfoClient;

    public NaverOAuthService(RefreshTokenEntityRepository refreshTokenEntityRepository,
                             UserEntityRepository userEntityRepository,
                             JwtService jwtService,
                             NaverUrlBuilder naverUrlBuilder,
                             NaverTokenClient naverTokenClient,
                             NaverUserInfoClient naverUserInfoClient) {
        super(userEntityRepository, refreshTokenEntityRepository, jwtService);
        this.naverUrlBuilder = naverUrlBuilder;
        this.naverTokenClient = naverTokenClient;
        this.naverUserInfoClient = naverUserInfoClient;
    }


    @Override
    protected UserEntity getUserEntity(UserInfoResponse userInfo) {
        NaverUserInfoResponse naverUserInfo = (NaverUserInfoResponse) userInfo;
        NaverUserInfoResponse.NaverAccount naverAccount = naverUserInfo.naverAccount();
        return userEntityRepository.findByEmail(naverAccount.email())
                .orElseGet(() -> userEntityRepository.save(UserEntity.builder()
                        .socialId(naverAccount.id())
                        .email(naverAccount.email())
                        .name(naverAccount.name())
                        .role(UserRole.USER)
                        .gender(naverAccount.gender())
                        .birthday(naverAccount.birthday())
                        .birthyear(naverAccount.birthyear())
                        .profileImageUrl(naverAccount.profileImageUrl())
                        .build()));
    }

    @Override
    protected UserInfoResponse getUserInfo(TokenResponse token) {
        String userInfoUrl = naverUrlBuilder.getUserInfoUrl();
        return naverUserInfoClient.getUserInfo(URI.create(userInfoUrl), token.authorizationToken());
    }

    @Override
    protected TokenResponse getToken(String code, String state) {
        String tokenUrl = naverUrlBuilder.getTokenUrl(code, state);
        return naverTokenClient.getToken(URI.create(tokenUrl));
    }

    @Override
    protected UserInfoResponse getUserInfo(OAuthLoginRequest request) {
        String userInfoUrl = naverUrlBuilder.getUserInfoUrl();
        return naverUserInfoClient.getUserInfo(URI.create(userInfoUrl), request.accessToken());
    }
}
