package com.moemoe.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moemoe.api.AbstractControllerTest;
import com.moemoe.api.config.handler.ErrorResponseBody;
import com.moemoe.core.request.RefreshAccessTokenRequest;
import com.moemoe.core.response.LoginTokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest extends AbstractControllerTest {
    @Test
    @DisplayName("성공 케이스 : Refresh Token 을 이용하여 AccessToken 재발급")
    void refresh() {
        // given
        String expectedRefreshToken = "expectedRefreshToken";
        String expectedAccessToken = "expectedAccessToken";
        LoginTokenResponse response = LoginTokenResponse.builder()
                .refreshToken(expectedRefreshToken)
                .accessToken(expectedAccessToken)
                .build();
        RefreshAccessTokenRequest refreshAccessTokenRequest = new RefreshAccessTokenRequest();
        ReflectionTestUtils.setField(refreshAccessTokenRequest, "refreshToken", expectedAccessToken);
        given(userService.refresh(argThat(req -> expectedAccessToken.equals(req.getRefreshToken()))))
                .willReturn(response);
        given(jwtService.getEmail(expectedAccessToken))
                .willReturn("user@moemoe.com");
        given(jwtService.getRole(expectedAccessToken))
                .willReturn("USER");
        given(jwtService.isValidToken(expectedRefreshToken, "user@moemoe.com"))
                .willReturn(true);

        // when
        String requestToJson = convertRequestToJson(refreshAccessTokenRequest);
        MockHttpServletRequestBuilder builder = post("/users/refresh")
                .content(requestToJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult invoke = invoke(builder, status().isOk(), false);
        LoginTokenResponse actualResponse = convertResponseToClass(invoke, LoginTokenResponse.class);

        // then
        assertThat(actualResponse)
                .isEqualTo(response);
    }

    @Test
    @DisplayName("실패 케이스 : Refresh Token이 빈 문자열 인 경우")
    void refreshWithoutToken() throws JsonProcessingException {
        // when
        RefreshAccessTokenRequest refreshAccessTokenRequest = new RefreshAccessTokenRequest();
        ReflectionTestUtils.setField(refreshAccessTokenRequest, "refreshToken", "refreshToken");
        String requestToJson = convertRequestToJson(refreshAccessTokenRequest);

        ObjectNode objectNode = (ObjectNode) objectMapper.readTree(requestToJson);
        objectNode.put("refreshToken", "");
        requestToJson = objectMapper.writeValueAsString(objectNode);
        MockHttpServletRequestBuilder builder = post("/users/refresh")
                .content(requestToJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult invoke = invoke(builder, status().isBadRequest(), false);
        ErrorResponseBody actualErrorResponse = convertResponseToClass(invoke, ErrorResponseBody.class);
        assertThat(actualErrorResponse)
                .extracting(ErrorResponseBody::getType)
                .isEqualTo(MethodArgumentNotValidException.class.getSimpleName());
    }
}