package com.moemoe.api.controller;

import com.moemoe.api.AbstractControllerTest;
import com.moemoe.api.config.handler.ErrorResponseBody;
import com.moemoe.core.response.LoginTokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest extends AbstractControllerTest {
    @Test
    @DisplayName("성공 케이스 : 헤더에 Authorization 이 포함된 경우")
    void refresh() {
        // given
        String expectedRefreshToken = "expectedRefreshToken";
        String expectedAccessToken = "expectedAccessToken";
        LoginTokenResponse response = LoginTokenResponse.builder()
                .refreshToken(expectedRefreshToken)
                .accessToken(expectedAccessToken)
                .build();
        given(userService.refresh(expectedRefreshToken))
                .willReturn(response);
        given(jwtService.getEmail(expectedAccessToken))
                .willReturn("user@moemoe.com");
        given(jwtService.getRole(expectedAccessToken))
                .willReturn("USER");
        given(jwtService.isValidToken(expectedRefreshToken, "user@moemoe.com"))
                .willReturn(true);

        // when
        MockHttpServletRequestBuilder builder = get("/users/refresh")
                .header("Authorization", "Bearer " + expectedRefreshToken);
        MvcResult invoke = invoke(builder, status().isOk(), false);
        LoginTokenResponse actualResponse = convertResponseToClass(invoke, LoginTokenResponse.class);

        // then
        assertThat(actualResponse)
                .isEqualTo(response);
    }

    @Test
    @DisplayName("실패 케이스 : 헤더에 Authorization 이 포함되지 않은 경우")
    void refreshWithoutToken() {
        // when
        MockHttpServletRequestBuilder builder = get("/users/refresh");

        MvcResult invoke = invoke(builder, status().isUnauthorized(), false);
        ErrorResponseBody actualErrorResponse = convertResponseToClass(invoke, ErrorResponseBody.class);
        assertThat(actualErrorResponse)
                .extracting(ErrorResponseBody::getType, ErrorResponseBody::getMessage)
                .containsExactly("EMPTY_AUTH_HEADER", "Authorization header is missing or empty.");
    }
}