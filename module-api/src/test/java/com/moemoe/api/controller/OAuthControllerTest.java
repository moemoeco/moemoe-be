package com.moemoe.api.controller;

import com.moemoe.api.AbstractControllerTest;
import com.moemoe.core.response.AuthorizationResponse;
import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.oauth.KakaoOAuthService;
import com.moemoe.core.service.oauth.NaverOAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
    @DisplayName("성공 케이스 : 카카오 로그인 페이지 URL 반환")
    void loginPageWithKakao() {
        String expectedUrl = "http://example.com";

        // given
        String expectedKakaoState = "";
        AuthorizationResponse expectedResponse = new AuthorizationResponse(expectedUrl + "?state=" + expectedKakaoState);
        given(kakaoOAuthService.authorize(expectedKakaoState))
                .willReturn(expectedResponse);

        // when
        MockHttpServletRequestBuilder builder = get("/oauth/kakao/login-page");
        MvcResult invoke = invoke(builder, status().isOk(), false);
        AuthorizationResponse actualResponse = convertResponseToClass(invoke, AuthorizationResponse.class);

        // then
        assertThat(actualResponse)
                .extracting(AuthorizationResponse::redirectUrl)
                .isEqualTo(expectedResponse.redirectUrl());
        verify(kakaoOAuthService, times(1))
                .authorize(anyString());
        verify(naverOAuthService, never())
                .authorize(anyString());
    }

    @Test
    @DisplayName("성공 케이스 : 네이버 로그인 페이지 URL 반환")
    void loginPageWithNaver() {
        String expectedUrl = "http://example.com";

        // given
        String expectedNaverState = "";
        AuthorizationResponse expectedResponse = new AuthorizationResponse(expectedUrl + "?state=" + expectedNaverState);
        given(naverOAuthService.authorize(expectedNaverState))
                .willReturn(expectedResponse);

        // when
        MockHttpServletRequestBuilder builder = get("/oauth/naver/login-page");
        MvcResult invoke = invoke(builder, status().isOk(), false);
        AuthorizationResponse actualResponse = convertResponseToClass(invoke, AuthorizationResponse.class);

        // then
        assertThat(actualResponse)
                .extracting(AuthorizationResponse::redirectUrl)
                .isEqualTo(expectedResponse.redirectUrl());

        // then
        verify(kakaoOAuthService, never())
                .authorize(anyString());
        verify(naverOAuthService, times(1))
                .authorize(anyString());
    }

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
        MvcResult invoke = invoke(builder, status().isOk(), false);
        LoginTokenResponse actualResponse = convertResponseToClass(invoke, LoginTokenResponse.class);

        // then
        assertThat(actualResponse)
                .extracting(LoginTokenResponse::refreshToken, LoginTokenResponse::accessToken)
                .containsExactly(expectedLoginTokenResponse.refreshToken(), expectedLoginTokenResponse.accessToken());

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
        MvcResult invoke = invoke(builder, status().isOk(), false);
        LoginTokenResponse actualResponse = convertResponseToClass(invoke, LoginTokenResponse.class);

        // then
        assertThat(actualResponse)
                .extracting(LoginTokenResponse::refreshToken, LoginTokenResponse::accessToken)
                .containsExactly(expectedLoginTokenResponse.refreshToken(), expectedLoginTokenResponse.accessToken());

        verify(kakaoOAuthService, never())
                .login(anyString(), anyString());
        verify(naverOAuthService, times(1))
                .login(anyString(), anyString());
    }
}