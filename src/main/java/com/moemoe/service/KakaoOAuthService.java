package com.moemoe.service;

import com.moemoe.domain.RefreshToken;
import com.moemoe.domain.User;
import com.moemoe.domain.UserRole;
import com.moemoe.dto.KakaoTokenResponse;
import com.moemoe.dto.KakaoUserInfoResponse;
import com.moemoe.dto.LoginTokenResponse;
import com.moemoe.http.KakaoTokenClient;
import com.moemoe.http.KakaoUserInfoClient;
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
    private final KakaoURLBuilder kakaoURLBuilder;
    private final KakaoTokenClient kakaoTokenClient;
    private final KakaoUserInfoClient kakaoUserInfoClient;
    private final UserEntityRepository userEntityRepository;
    private final RefreshTokenEntityRepository refreshTokenEntityRepository;
    private final JwtService jwtService;

    public String authorize() {
        return kakaoURLBuilder.authorize();
    }

    @Transactional
    public LoginTokenResponse login(String code) {
        KakaoTokenResponse token = getToken(code);
        KakaoUserInfoResponse userInfo = getKakaoUserInfoResponse(token);
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

    private User getUserEntity(KakaoUserInfoResponse userInfo) {
        KakaoUserInfoResponse.KakaoAccount kakaoAccount = userInfo.kakaoAccount();
        KakaoUserInfoResponse.Profile profile = kakaoAccount.profile();
        return userEntityRepository.findByEmail(kakaoAccount.email())
                .orElseGet(() -> userEntityRepository.save(User.builder()
                        .socialId(userInfo.id())
                        .email(kakaoAccount.email())
                        .name(kakaoAccount.name())
                        .role(UserRole.USER)
                        .gender(kakaoAccount.gender())
                        .birthday(kakaoAccount.birthday())
                        .birthyear(kakaoAccount.birthyear())
                        .profileImageUrl(profile.profileImageUrl())
                        .build()));
    }

    private KakaoUserInfoResponse getKakaoUserInfoResponse(KakaoTokenResponse token) {
        String userInfoUrl = kakaoURLBuilder.getUserInfo();
        return kakaoUserInfoClient.getUserInfo(URI.create(userInfoUrl), token.getAuthorizationToken());
    }

    private KakaoTokenResponse getToken(String code) {
        String tokenUrl = kakaoURLBuilder.getToken(code);
        return kakaoTokenClient.getToken(URI.create(tokenUrl));
    }
}
