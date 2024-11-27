package com.moemoe.client.http.kakao;

import com.moemoe.client.http.AbstractMockWebServer;
import com.moemoe.client.http.dto.kakao.KakaoTokenResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoTokenClientTest extends AbstractMockWebServer {
    @Autowired
    private KakaoTokenClient kakaoTokenClient;

    @Test
    void getToken() {
        // given
        String expectedUrl = "/oauth/token";

        // ExpectedKaKaoTokenResponse Builder example
        ExpectedKaKaoTokenResponse expected = ExpectedKaKaoTokenResponse.builder()
                .token_type("bearer")
                .access_token("sampleAccessToken123")
                .refresh_token("sampleRefreshToken456")
                .expires_in(3600)
                .id_token("id_token")
                .refresh_token_expires_in(7200)
                .build();

        //  Content type 'application/x-www-form-urlencoded' not supported for bodyType=
        mockResponse(expected, Map.of("Content-Type", MediaType.APPLICATION_JSON));

        KakaoTokenResponse actual = kakaoTokenClient.getToken(getUri(expectedUrl));

        // actual equal expected
        assertThat(actual.tokenType()).isEqualTo(expected.getToken_type());
        assertThat(actual.accessToken()).isEqualTo(expected.getAccess_token());
        assertThat(actual.refreshToken()).isEqualTo(expected.getRefresh_token());
        assertThat(actual.expiresIn()).isEqualTo(expected.getExpires_in());
        assertThat(actual.idToken()).isEqualTo(expected.getId_token());
        assertThat(actual.refreshTokenExpiresIn()).isEqualTo(expected.getRefresh_token_expires_in());
    }

    @Getter
    @Builder
    @AllArgsConstructor
    static class ExpectedKaKaoTokenResponse {
        private String token_type;
        private String access_token;
        private String refresh_token;
        private int expires_in;
        private String id_token;
        private int refresh_token_expires_in;
    }
}