package com.moemoe.api.controller;

import com.moemoe.api.AbstractControllerTest;
import com.moemoe.core.response.LoginTokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest extends AbstractControllerTest {

    @Test
    @DisplayName("성공 케이스 : 헤더에 Authorization 이 포함된 경우")
    void refresh() throws Exception {
        // given
        String expectedRefreshToken = "expectedRefreshToken";
        String expectedAccessToken = "expectedAccessToken";
        LoginTokenResponse response = LoginTokenResponse.builder()
                .refreshToken(expectedRefreshToken)
                .accessToken(expectedAccessToken)
                .build();
        given(jwtService.refresh(expectedRefreshToken))
                .willReturn(response);
        given(jwtService.getEmail(expectedAccessToken))
                .willReturn("user@moemoe.com");
        given(jwtService.getRole(expectedAccessToken))
                .willReturn("USER");
        given(jwtService.isValidToken(expectedRefreshToken, "user@moemoe.com"))
                .willReturn(true);

        // when
        ResultActions resultActions = mockMvc.perform(get("/users/refresh")
                        .header("Authorization", "Bearer " + expectedRefreshToken))
                .andExpect(status().isOk())
                .andDo(print());
        LoginTokenResponse actualResponse = convertResponseToClass(resultActions, LoginTokenResponse.class);

        // then
        assertThat(actualResponse)
                .isEqualTo(response);
        verify(jwtService, times(1))
                .refresh(expectedRefreshToken);
    }

    @Test
    @DisplayName("실패 케이스 : 헤더에 Authorization 이 포함되지 않은 경우")
    void refreshWithoutToken() throws Exception {
        // when
        mockMvc.perform(get("/users/refresh"))
                .andExpect(status().isUnauthorized())
                .andDo(print());
    }
}