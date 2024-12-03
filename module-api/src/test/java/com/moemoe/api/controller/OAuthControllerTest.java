package com.moemoe.api.controller;

import com.moemoe.api.ResponseConvertUtil;
import com.moemoe.api.config.filter.JwtAuthenticationFilter;
import com.moemoe.api.config.web.SecurityConfig;
import com.moemoe.api.config.web.WebConfig;
import com.moemoe.core.dto.AuthorizationResponse;
import com.moemoe.core.dto.LoginTokenResponse;
import com.moemoe.core.service.oauth.KakaoOAuthService;
import com.moemoe.core.service.oauth.NaverOAuthService;
import com.moemoe.mongo.repository.UserEntityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OAuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        excludeAutoConfiguration = {SecurityConfig.class, WebConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class OAuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserEntityRepository userEntityRepository;
    @MockBean
    private KakaoOAuthService kakaoOAuthService;
    @MockBean
    private NaverOAuthService naverOAuthService;

    @Test
    void loginPageWithKakao() throws Exception {
        String expectedUrl = "http://example.com";

        // given
        String expectedKakaoState = "";
        AuthorizationResponse expectedResponse = new AuthorizationResponse(expectedUrl + "?state=" + expectedKakaoState);
        given(kakaoOAuthService.authorize(expectedKakaoState))
                .willReturn(expectedResponse);

        // when
        ResultActions resultActions = mockMvc.perform(get("/oauth/kakao/login-page"))
                .andExpect(status().isOk())
                .andDo(print());

        AuthorizationResponse actualResponse = ResponseConvertUtil.convertResponseToClass(resultActions, AuthorizationResponse.class);

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
    void loginPageWithNaver() throws Exception {
        String expectedUrl = "http://example.com";

        // given
        String expectedNaverState = "";
        AuthorizationResponse expectedResponse = new AuthorizationResponse(expectedUrl + "?state=" + expectedNaverState);
        given(naverOAuthService.authorize(expectedNaverState))
                .willReturn(expectedResponse);

        // when
        ResultActions resultActions = mockMvc.perform(get("/oauth/naver/login-page"))
                .andExpect(status().isOk())
                .andDo(print());
        AuthorizationResponse actualResponse = ResponseConvertUtil.convertResponseToClass(resultActions, AuthorizationResponse.class);

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
    void loginWithKakao() throws Exception {
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
        ResultActions resultActions = mockMvc.perform(get("/oauth/kakao/login")
                        .param("code", kakaoExpectedCode)
                        .param("state", kakaoExpectedState))
                .andExpect(status().isOk())
                .andDo(print());

        LoginTokenResponse actualResponse = ResponseConvertUtil.convertResponseToClass(resultActions, LoginTokenResponse.class);

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
    void loginWithNaver() throws Exception {
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
        ResultActions resultActions = mockMvc.perform(get("/oauth/naver/login")
                        .param("code", naverExpectedCode)
                        .param("state", naverExpectedState))
                .andExpect(status().isOk())
                .andDo(print());

        LoginTokenResponse actualResponse = ResponseConvertUtil.convertResponseToClass(resultActions, LoginTokenResponse.class);

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