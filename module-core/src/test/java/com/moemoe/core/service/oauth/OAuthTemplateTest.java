package com.moemoe.core.service.oauth;

import com.moemoe.client.http.dto.UserInfoResponse;
import com.moemoe.client.http.dto.kakao.KakaoUserInfoResponse;
import com.moemoe.client.http.dto.naver.NaverUserInfoResponse;
import com.moemoe.client.http.kakao.KakaoUserInfoClient;
import com.moemoe.client.http.naver.NaverUserInfoClient;
import com.moemoe.core.request.OAuthLoginRequest;
import com.moemoe.core.service.builder.KakaoUrlBuilder;
import com.moemoe.core.service.builder.NaverUrlBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OAuthTemplateTest {
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