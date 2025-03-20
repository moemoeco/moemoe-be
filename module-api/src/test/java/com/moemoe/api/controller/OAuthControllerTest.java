package com.moemoe.api.controller;

import com.moemoe.api.AbstractControllerTest;
import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.oauth.KakaoOAuthService;
import com.moemoe.core.service.oauth.NaverOAuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OAuthController.class)
class OAuthControllerTest extends AbstractControllerTest {
    @MockBean
    private KakaoOAuthService kakaoOAuthService;
    @MockBean
    private NaverOAuthService naverOAuthService;

    @Test
    @DisplayName("성공 케이스 : 카카오 로그인 호출")
    void loginWithKakao() {
        // given
        String kakaoExpectedCode = "kakao";
        String kakaoExpectedState = "";
        LoginTokenResponse expectedLoginTokenResponse = LoginTokenResponse.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .build();
        given(kakaoOAuthService.login(kakaoExpectedCode, kakaoExpectedState))
                .willReturn(expectedLoginTokenResponse);

        // when
        MockHttpServletRequestBuilder builder = get("/oauth/kakao/login")
                .param("code", kakaoExpectedCode)
                .param("state", kakaoExpectedState);
        MvcResult invoke = invoke(builder, status().isFound(), false);
        MockHttpServletResponse response = invoke.getResponse();
        String redirectedUrl = response.getRedirectedUrl();
        Cookie refreshToken = response.getCookie("refreshToken");
        Cookie accessToken = response.getCookie("accessToken");

        // then
        assertThat(refreshToken)
                .extracting(Cookie::getValue)
                .isEqualTo(expectedLoginTokenResponse.refreshToken());
        assertThat(accessToken)
                .extracting(Cookie::getValue)
                .isEqualTo(expectedLoginTokenResponse.accessToken());
        assertThat(redirectedUrl)
                .isEqualTo("http://localhost:8081?redirectedFromSocialLogin=true");

        verify(kakaoOAuthService, times(1))
                .login(anyString(), anyString());
        verify(naverOAuthService, never())
                .login(anyString(), anyString());
    }

    @Test
    @DisplayName("성공 케이스 : 네이버 로그인 호출")
    void loginWithNaver() {
        // given
        String naverExpectedCode = "naver";
        String naverExpectedState = "";
        LoginTokenResponse expectedLoginTokenResponse = LoginTokenResponse.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .build();
        given(naverOAuthService.login(naverExpectedCode, naverExpectedState))
                .willReturn(expectedLoginTokenResponse);

        // when
        MockHttpServletRequestBuilder builder = get("/oauth/naver/login")
                .param("code", naverExpectedCode)
                .param("state", naverExpectedState);
        MvcResult invoke = invoke(builder, status().isFound(), false);
        MockHttpServletResponse response = invoke.getResponse();
        String redirectedUrl = response.getRedirectedUrl();
        Cookie refreshToken = response.getCookie("refreshToken");
        Cookie accessToken = response.getCookie("accessToken");

        // then
        assertThat(refreshToken)
                .extracting(Cookie::getValue)
                .isEqualTo(expectedLoginTokenResponse.refreshToken());
        assertThat(accessToken)
                .extracting(Cookie::getValue)
                .isEqualTo(expectedLoginTokenResponse.accessToken());
        assertThat(redirectedUrl)
                .isEqualTo("http://localhost:8081?redirectedFromSocialLogin=true");

        verify(kakaoOAuthService, never())
                .login(anyString(), anyString());
        verify(naverOAuthService, times(1))
                .login(anyString(), anyString());
    }
}