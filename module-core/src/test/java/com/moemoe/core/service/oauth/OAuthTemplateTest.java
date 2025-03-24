package com.moemoe.core.service.oauth;

import com.moemoe.client.http.dto.UserInfoResponse;
import com.moemoe.client.http.dto.kakao.KakaoUserInfoResponse;
import com.moemoe.client.http.dto.naver.NaverUserInfoResponse;
import com.moemoe.client.http.kakao.KakaoUserInfoClient;
import com.moemoe.client.http.naver.NaverUserInfoClient;
import com.moemoe.core.request.OAuthLoginRequest;
import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.builder.KakaoUrlBuilder;
import com.moemoe.core.service.builder.NaverUrlBuilder;
import com.moemoe.core.service.jwt.JwtService;
import com.moemoe.mongo.constant.UserRole;
import com.moemoe.mongo.entity.UserEntity;
import com.moemoe.mongo.repository.UserEntityRepository;
import com.moemoe.redis.entity.RefreshTokenEntity;
import com.moemoe.redis.repository.RefreshTokenEntityRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthTemplateTest {
    @Test
    @DisplayName("성공 케이스 : OAuth 로그인")
    void login() {
        // given
        UserEntityRepository userEntityRepository = mock(UserEntityRepository.class);
        RefreshTokenEntityRepository refreshTokenEntityRepository = mock(RefreshTokenEntityRepository.class);
        JwtService jwtService = mock(JwtService.class);
        MockSettings mockSettings = withSettings()
                .useConstructor(userEntityRepository, refreshTokenEntityRepository, jwtService)
                .defaultAnswer(CALLS_REAL_METHODS);
        OAuthTemplate mockOAuthTemplate = mock(OAuthTemplate.class, mockSettings);

        String tokenType = "bearer";
        String token = "access_token";
        OAuthLoginRequest request = new OAuthLoginRequest(tokenType, token);

        String email = "moe@example.com";
        UserRole userRole = UserRole.USER;
        UserEntity userEntity = UserEntity.builder()
                .role(userRole)
                .email(email)
                .build();
        ObjectId id = new ObjectId();
        ReflectionTestUtils.setField(userEntity, "id", id);
        WrapUserInfoResponse wrapUserInfoResponse = new WrapUserInfoResponse(email, userRole);
        given(mockOAuthTemplate.getUserInfo(request))
                .willReturn(wrapUserInfoResponse);
        given(mockOAuthTemplate.getUserEntity(wrapUserInfoResponse))
                .willReturn(userEntity);

        Map<String, String> claims = Map.of("email", email, "role", userRole.name());
        given(jwtService.createRefreshToken(claims, id.toHexString()))
                .willReturn("refreshToken");
        given(jwtService.createAccessToken(claims, id.toHexString()))
                .willReturn("accessToken");

        given(refreshTokenEntityRepository.findById(email))
                .willReturn(Optional.empty());

        // when
        LoginTokenResponse response = mockOAuthTemplate.login(request);

        // then
        assertThat(response)
                .extracting(LoginTokenResponse::accessToken, LoginTokenResponse::refreshToken)
                .containsExactly("accessToken", "refreshToken");

        verify(refreshTokenEntityRepository, times(1))
                .save(any(RefreshTokenEntity.class));
    }

    static class WrapUserInfoResponse implements UserInfoResponse {
        private String email;
        private UserRole userRole;

        public WrapUserInfoResponse(String email, UserRole userRole) {
            this.email = email;
            this.userRole = userRole;
        }
    }

    @Nested
    @DisplayName("네이버 OAuth 템플릿 테스트")
    class NaverOAuthTemplateTest {
        @InjectMocks
        private NaverOAuthService naverOAuthService;
        @Mock
        private NaverUrlBuilder naverUrlBuilder;
        @Mock
        private NaverUserInfoClient naverUserInfoClient;

        @Test
        @DisplayName("성공 케이스 : 네이버 유저 정보 조회")
        void getUserInfo() {
            // given
            OAuthLoginRequest request = new OAuthLoginRequest("Bearer", "token");
            String expectedUserInfoUri = "/naver/me";
            given(naverUrlBuilder.getUserInfoUrl())
                    .willReturn(expectedUserInfoUri);
            NaverUserInfoResponse.NaverAccount naverAccount = new NaverUserInfoResponse.NaverAccount(
                    "123456",
                    "https://example.com/profile.png",
                    "M",
                    "test@example.com",
                    "010-1234-5678",
                    "+821012345678",
                    "홍길동",
                    "0101",
                    "1990"
            );
            NaverUserInfoResponse response = new NaverUserInfoResponse("", "", naverAccount);
            given(naverUserInfoClient.getUserInfo(URI.create(expectedUserInfoUri), request.accessToken()))
                    .willReturn(response);

            // when
            UserInfoResponse userInfo = naverOAuthService.getUserInfo(request);

            // then
            assertThat(userInfo)
                    .isEqualTo(response);
        }
    }

    @Nested
    @DisplayName("카카오 OAuth 템플릿 테스트")
    class KakaoOAuthTemplateTest {
        @InjectMocks
        private KakaoOAuthService kakaoOAuthService;
        @Mock
        private KakaoUrlBuilder kakaoUrlBuilder;
        @Mock
        private KakaoUserInfoClient kakaoUserInfoClient;

        @Test
        @DisplayName("성공 케이스 : 네이버 유저 정보 조회")
        void getUserInfo() {
            // given
            OAuthLoginRequest request = new OAuthLoginRequest("Bearer", "token");
            String expectedUserInfoUri = "/kakao/me";
            given(kakaoUrlBuilder.getUserInfoUrl())
                    .willReturn(expectedUserInfoUri);
            KakaoUserInfoResponse.KakaoAccount.Profile profile = new KakaoUserInfoResponse.KakaoAccount.Profile(
                    "TestNickname",
                    "https://example.com/profile.jpg"
            );
            KakaoUserInfoResponse.KakaoAccount kakaoAccount = new KakaoUserInfoResponse.KakaoAccount(
                    profile,
                    "Test Name",
                    "test@example.com",
                    "1990",
                    "0101",
                    "SOLAR",
                    "M"
            );
            KakaoUserInfoResponse response = new KakaoUserInfoResponse("id", "connectedAt", kakaoAccount);
            given(kakaoUserInfoClient.getUserInfo(URI.create(expectedUserInfoUri), request.accessToken()))
                    .willReturn(response);

            // when
            UserInfoResponse userInfo = kakaoOAuthService.getUserInfo(request);

            // then
            assertThat(userInfo)
                    .isEqualTo(response);
        }
    }
}