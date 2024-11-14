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
        String expectedUrl = "http://authorization-uri" +
                "?response_type=code" +
                "&client_id=client-id" +
                "&redirect_uri=redirect-uri" +
                "&state=state" +
                "&scope=profile_nickname,name";
        String actualUrl = kakaoURLBuilder.authorize("state");

        assertThat(actualUrl)
                .isEqualTo(expectedUrl);
    }

    @Test
    void getToken() {
        String expectedUrl = "http://token-uri" +
                "?grant_type=authorization_code" +
                "&client_id=client-id" +
                "&redirect_uri=redirect-uri" +
                "&code=test" +
                "&state=state" +
                "&client_secret=client-secret";

        String actualUrl = kakaoURLBuilder.getToken("test", "state");

        assertThat(actualUrl)
                .isEqualTo(expectedUrl);
    }

    @Test
    void getUserInfo() {
        String expectedUrl = "http://user-info-uri" +
                "?secure_resource=true";

        String actualUrl = kakaoURLBuilder.getUserInfo();

        assertThat(actualUrl)
                .isEqualTo(expectedUrl);
    }
}