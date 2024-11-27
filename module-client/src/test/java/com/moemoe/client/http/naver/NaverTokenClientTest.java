package com.moemoe.client.http.naver;

import com.moemoe.client.http.AbstractMockWebServer;
import com.moemoe.client.http.dto.naver.NaverTokenResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NaverTokenClientTest extends AbstractMockWebServer {
    @Autowired
    private NaverTokenClient naverTokenClient;

    @Test
    void getToken() {
        // given
        String expectedUrl = "/oauth2.0/token";

        ExpectedNaverTokenResponse expectedTokenResponse = ExpectedNaverTokenResponse.builder()
                .token_type("bearer")
                .access_token("sampleAccessToken123")
                .refresh_token("sampleRefreshToken456")
                .expires_in(3600)
                .error("null")
                .error_description("No error")
                .build();
        //  Content type 'application/x-www-form-urlencoded' not supported for bodyType=
        mockResponse(expectedTokenResponse, Map.of("Content-Type", MediaType.APPLICATION_JSON));


        // when
        NaverTokenResponse actualTokenResponse = naverTokenClient.getToken(getUri(expectedUrl));

        assertThat(actualTokenResponse.tokenType()).isEqualTo(expectedTokenResponse.getToken_type());
        assertThat(actualTokenResponse.accessToken()).isEqualTo(expectedTokenResponse.getAccess_token());
        assertThat(actualTokenResponse.refreshToken()).isEqualTo(expectedTokenResponse.getRefresh_token());
        assertThat(actualTokenResponse.expiresIn()).isEqualTo(expectedTokenResponse.getExpires_in());
        assertThat(actualTokenResponse.errorMsg()).isEqualTo(expectedTokenResponse.getError());
        assertThat(actualTokenResponse.errorDescription()).isEqualTo(expectedTokenResponse.getError_description());
    }

    @Getter
    @Builder
    @AllArgsConstructor
    static class ExpectedNaverTokenResponse {
        private String token_type;
        private String access_token;
        private String refresh_token;
        private int expires_in;
        private String error;
        private String error_description;
    }

}

