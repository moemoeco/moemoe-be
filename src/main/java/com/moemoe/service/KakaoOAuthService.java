package com.moemoe.service;

import com.moemoe.domain.RefreshToken;
import com.moemoe.domain.User;
import com.moemoe.domain.UserRole;
import com.moemoe.dto.kakao.KakaoUserInfoResponse;
import com.moemoe.dto.LoginTokenResponse;
import com.moemoe.dto.TokenResponse;
import com.moemoe.dto.UserInfoResponse;
import com.moemoe.http.builder.UrlBuilder;
import com.moemoe.http.client.kakao.KakaoTokenClient;
import com.moemoe.http.client.kakao.KakaoUserInfoClient;
import com.moemoe.repository.RefreshTokenEntityRepository;
import com.moemoe.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class KakaoOAuthService {
    private final UrlBuilder kakaoUrlBuilder;
    private final KakaoTokenClient kakaoTokenClient;
    private final KakaoUserInfoClient kakaoUserInfoClient;
    private final UserEntityRepository userEntityRepository;
    private final RefreshTokenEntityRepository refreshTokenEntityRepository;
    private final JwtService jwtService;

    public String authorize(String state) {
        return kakaoUrlBuilder.getAuthorizeUrl(state);
    }

    @Transactional
    public LoginTokenResponse login(String code, String state) {
        TokenResponse token = getToken(code, state);
        UserInfoResponse userInfo = getUserInfo(token);
        User userEntity = getUserEntity(userInfo);

        Map<String, String> claims = new HashMap<>();
        claims.put("email", userEntity.getEmail());
        claims.put("role", userEntity.getRole().name());

        final String accessToken = jwtService.createAccessToken(claims, userEntity);
        final String refreshToken = jwtService.createRefreshToken(claims, userEntity);

        refreshTokenEntityRepository.save(RefreshToken.of(userEntity.getEmail(), refreshToken));
        return LoginTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private User getUserEntity(UserInfoResponse userInfo) {
        KakaoUserInfoResponse kakaoUserInfo = (KakaoUserInfoResponse) userInfo;
        KakaoUserInfoResponse.KakaoAccount kakaoAccount = kakaoUserInfo.kakaoAccount();
        KakaoUserInfoResponse.Profile profile = kakaoAccount.profile();
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

    private UserInfoResponse getUserInfo(TokenResponse token) {
        String userInfoUrl = kakaoUrlBuilder.getUserInfoUrl();
        return kakaoUserInfoClient.getUserInfo(URI.create(userInfoUrl), token.authorizationToken());
    }

    private TokenResponse getToken(String code, String state) {
        String tokenUrl = kakaoUrlBuilder.getTokenUrl(code, state);
        return kakaoTokenClient.getToken(URI.create(tokenUrl));
    }
}
