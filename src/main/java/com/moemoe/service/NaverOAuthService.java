package com.moemoe.service;

import com.moemoe.domain.mongo.User;
import com.moemoe.domain.mongo.UserRole;
import com.moemoe.dto.TokenResponse;
import com.moemoe.dto.UserInfoResponse;
import com.moemoe.dto.naver.NaverUserInfoResponse;
import com.moemoe.http.builder.NaverUrlBuilder;
import com.moemoe.http.builder.UrlBuilder;
import com.moemoe.http.client.naver.NaverTokenClient;
import com.moemoe.http.client.naver.NaverUserInfoClient;
import com.moemoe.repository.redis.RefreshTokenEntityRepository;
import com.moemoe.repository.mongo.UserEntityRepository;
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
    public String authorize(String state) {
        return naverUrlBuilder.getAuthorizeUrl(state);
    }

    @Override
    protected User getUserEntity(UserInfoResponse userInfo) {
        NaverUserInfoResponse naverUserInfo = (NaverUserInfoResponse) userInfo;
        NaverUserInfoResponse.NaverAccount naverAccount = naverUserInfo.naverAccount();
        return userEntityRepository.findByEmail(naverAccount.email())
                .orElseGet(() -> userEntityRepository.save(User.builder()
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
}
