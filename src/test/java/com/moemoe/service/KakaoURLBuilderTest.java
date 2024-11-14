package com.moemoe.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {KakaoURLBuilder.class})
@TestPropertySource(properties = {
        "spring.security.oauth2.client.provider.kakao.authorization-uri=http://authorization-uri",
        "spring.security.oauth2.client.provider.kakao.token-uri=http://token-uri",
        "spring.security.oauth2.client.provider.kakao.user-info-uri=http://user-info-uri",
        "spring.security.oauth2.client.registration.kakao.client-id=client-id",
        "spring.security.oauth2.client.registration.kakao.client-secret=client-secret",
        "spring.security.oauth2.client.registration.kakao.redirect-uri=redirect-uri",
        "spring.security.oauth2.client.registration.kakao.scope=profile_nickname,name"
})
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