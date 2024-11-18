package com.moemoe.service;

import com.moemoe.domain.RefreshToken;
import com.moemoe.domain.User;
import com.moemoe.domain.UserRole;
import com.moemoe.dto.LoginTokenResponse;
import com.moemoe.dto.NaverTokenResponse;
import com.moemoe.dto.NaverUserInfoResponse;
import com.moemoe.http.client.naver.NaverTokenClient;
import com.moemoe.http.client.naver.NaverUserInfoClient;
import com.moemoe.http.builder.UrlBuilder;
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
public class NaverOAuthService {
    private final UrlBuilder naverUrlBuilder;
    private final NaverTokenClient naverTokenClient;
    private final NaverUserInfoClient naverUserInfoClient;
    private final UserEntityRepository userEntityRepository;
    private final RefreshTokenEntityRepository refreshTokenEntityRepository;
    private final JwtService jwtService;

    public String authorize(String state) {
        return naverUrlBuilder.getAuthorizeUrl(state);
    }

    @Transactional
    public LoginTokenResponse login(String code, String state) {
        NaverTokenResponse token = getToken(code, state);
        NaverUserInfoResponse userInfo = getUserInfo(token);
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

    private User getUserEntity(NaverUserInfoResponse userInfo) {
        NaverUserInfoResponse.NaverAccount naverAccount = userInfo.naverAccount();
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

    private NaverUserInfoResponse getUserInfo(NaverTokenResponse token) {
        String userInfoUrl = naverUrlBuilder.getUserInfoUrl();
        return naverUserInfoClient.getUserInfo(URI.create(userInfoUrl), token.getAuthorizationToken());
    }

    private NaverTokenResponse getToken(String code, String state) {
        String tokenUrl = naverUrlBuilder.getTokenUrl(code, state);
        return naverTokenClient.getToken(URI.create(tokenUrl));
    }
}
