package com.moemoe.api.controller;

import com.moemoe.api.AbstractControllerTest;
import com.moemoe.core.request.OAuthLoginRequest;
import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.oauth.KakaoOAuthService;
import com.moemoe.core.service.oauth.NaverOAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        String tokenType = "Bearer";
        String kakaoExpectedToken = "kakaoExpectedToken";
        OAuthLoginRequest request = new OAuthLoginRequest(tokenType, kakaoExpectedToken);
        LoginTokenResponse expectedLoginTokenResponse = LoginTokenResponse.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .build();
        given(kakaoOAuthService.login(request))
                .willReturn(expectedLoginTokenResponse);

        // when
        String requestToJson = convertObjectToJson(request);
        MockHttpServletRequestBuilder builder = post("/oauth/kakao/login")
                .content(requestToJson)
                .contentType(MediaType.APPLICATION_JSON);
        MvcResult invoke = invoke(builder, status().isOk(), false);

        // then
        LoginTokenResponse response = convertResponseToClass(invoke, LoginTokenResponse.class);
        assertThat(response)
                .extracting(
                        LoginTokenResponse::accessToken,
                        LoginTokenResponse::refreshToken
                )
                .containsExactly(
                        expectedLoginTokenResponse.accessToken(),
                        expectedLoginTokenResponse.refreshToken()
                );

        verify(kakaoOAuthService, times(1))
                .login(request);
        verify(naverOAuthService, never())
                .login(request);
    }

    @Test
    @DisplayName("성공 케이스 : 네이버 로그인 호출")
    void loginWithNaver() {
        // given
        String tokenType = "Bearer";
        String naverExpectedToken = "naverExpectedToken";
        OAuthLoginRequest request = new OAuthLoginRequest(tokenType, naverExpectedToken);
        LoginTokenResponse expectedLoginTokenResponse = LoginTokenResponse.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .build();
        given(naverOAuthService.login(request))
                .willReturn(expectedLoginTokenResponse);

        // when
        String requestToJson = convertObjectToJson(request);
        MockHttpServletRequestBuilder builder = post("/oauth/naver/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestToJson);
        MvcResult invoke = invoke(builder, status().isOk(), false);

        // then
        LoginTokenResponse response = convertResponseToClass(invoke, LoginTokenResponse.class);
        assertThat(response)
                .extracting(
                        LoginTokenResponse::accessToken,
                        LoginTokenResponse::refreshToken
                )
                .containsExactly(
                        expectedLoginTokenResponse.accessToken(),
                        expectedLoginTokenResponse.refreshToken()
                );

        verify(kakaoOAuthService, never())
                .login(request);
        verify(naverOAuthService, times(1))
                .login(request);
    }
}