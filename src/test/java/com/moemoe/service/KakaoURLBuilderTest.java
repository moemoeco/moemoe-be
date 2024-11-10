package com.moemoe.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class KakaoURLBuilderTest {
    @Autowired
    private KakaoURLBuilder kakaoURLBuilder;

    @Test
    void authorize() {
        String expectedUrl = "https://kauth.kakao.com/oauth/authorize" +
                "?response_type=code" +
                "&client_id=client-id" +
                "&redirect_uri=redirect-uri" +
                "&scope=profile_nickname,name";
        String actualUrl = kakaoURLBuilder.authorize();

        assertThat(actualUrl)
                .isEqualTo(expectedUrl);
    }

    @Test
    void getToken() {
        String expectedUrl = "https://kauth.kakao.com/oauth/token" +
                "?grant_type=authorization_code" +
                "&client_id=client-id" +
                "&redirect_uri=redirect-uri" +
                "&code=test" +
                "&client_secret=client-secret";

        String actualUrl = kakaoURLBuilder.getToken("test");

        assertThat(actualUrl)
                .isEqualTo(expectedUrl);
    }

    @Test
    void getUserInfo() {
        String expectedUrl = "https://kapi.kakao.com/v2/user/me" +
                "?secure_resource=true";

        String actualUrl = kakaoURLBuilder.getUserInfo();

        assertThat(actualUrl)
                .isEqualTo(expectedUrl);
    }
}