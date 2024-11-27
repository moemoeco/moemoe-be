package com.moemoe.core.service.oauth;


import com.moemoe.client.http.dto.TokenResponse;
import com.moemoe.client.http.dto.UserInfoResponse;
import com.moemoe.client.http.dto.kakao.KakaoUserInfoResponse;
import com.moemoe.client.http.kakao.KakaoTokenClient;
import com.moemoe.client.http.kakao.KakaoUserInfoClient;
import com.moemoe.core.service.builder.KakaoUrlBuilder;
import com.moemoe.core.service.builder.UrlBuilder;
import com.moemoe.core.service.jwt.JwtService;
import com.moemoe.mongo.constant.UserRole;
import com.moemoe.mongo.entity.User;
import com.moemoe.mongo.repository.UserEntityRepository;
import com.moemoe.redis.repository.RefreshTokenEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;

@Slf4j
@Service
public class KakaoOAuthService extends OAuthTemplate {
    private final UrlBuilder kakaoUrlBuilder;
    private final KakaoTokenClient kakaoTokenClient;
    private final KakaoUserInfoClient kakaoUserInfoClient;

    protected KakaoOAuthService(RefreshTokenEntityRepository refreshTokenEntityRepository,
                                UserEntityRepository userEntityRepository,
                                JwtService jwtService,
                                KakaoUrlBuilder kakaoUrlBuilder,
                                KakaoTokenClient kakaoTokenClient,
                                KakaoUserInfoClient kakaoUserInfoClient) {
        super(userEntityRepository, refreshTokenEntityRepository, jwtService);
        this.kakaoUrlBuilder = kakaoUrlBuilder;
        this.kakaoTokenClient = kakaoTokenClient;
        this.kakaoUserInfoClient = kakaoUserInfoClient;
    }

    @Override
    public String authorize(String state) {
        return kakaoUrlBuilder.getAuthorizeUrl(state);
    }

    @Override
    protected User getUserEntity(UserInfoResponse userInfo) {
        KakaoUserInfoResponse kakaoUserInfo = (KakaoUserInfoResponse) userInfo;
        KakaoUserInfoResponse.KakaoAccount kakaoAccount = kakaoUserInfo.kakaoAccount();
        KakaoUserInfoResponse.KakaoAccount.Profile profile = kakaoAccount.profile();
        return userEntityRepository.findByEmail(kakaoAccount.email())
                .orElseGet(() -> userEntityRepository.save(User.builder()
                        .socialId(String.valueOf(kakaoUserInfo.id()))
                        .email(kakaoAccount.email())
                        .name(kakaoAccount.name())
                        .role(UserRole.USER)
                        .gender(kakaoAccount.gender())
                        .birthday(kakaoAccount.birthday())
                        .birthyear(kakaoAccount.birthyear())
                        .profileImageUrl(profile.profileImageUrl())
                        .build()));
    }

    @Override
    protected UserInfoResponse getUserInfo(TokenResponse token) {
        String userInfoUrl = kakaoUrlBuilder.getUserInfoUrl();
        return kakaoUserInfoClient.getUserInfo(URI.create(userInfoUrl), token.authorizationToken());
    }

    @Override
    protected TokenResponse getToken(String code, String state) {
        String tokenUrl = kakaoUrlBuilder.getTokenUrl(code, state);
        return kakaoTokenClient.getToken(URI.create(tokenUrl));
    }
}
