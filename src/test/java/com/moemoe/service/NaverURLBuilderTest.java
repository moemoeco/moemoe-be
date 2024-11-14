package com.moemoe.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {NaverURLBuilder.class})
@TestPropertySource(properties = {
        "spring.security.oauth2.client.provider.naver.authorization-uri=http://authorization-uri",
        "spring.security.oauth2.client.provider.naver.token-uri=http://token-uri",
        "spring.security.oauth2.client.provider.naver.user-info-uri=http://user-info-uri",
        "spring.security.oauth2.client.registration.naver.client-id=client-id",
        "spring.security.oauth2.client.registration.naver.client-secret=client-secret",
        "spring.security.oauth2.client.registration.naver.redirect-uri=redirect-uri",
        "spring.security.oauth2.client.registration.naver.authorization-grant-type=grant-type"
})
class NaverURLBuilderTest {
    @Autowired
    private NaverURLBuilder naverURLBuilder;

    @Test
    void authorize() {
        String expectedState = "test";
        String expectedUrl = "http://authorization-uri" +
                "?response_type=code" +
                "&client_id=client-id" +
                "&redirect_uri=redirect-uri" +
                "&state=" + expectedState;
        String actualUrl = naverURLBuilder.authorize(expectedState);

        assertThat(actualUrl)
                .isEqualTo(expectedUrl);
    }

    @Test
    void getToken() {
        String expectedCode = "code";
        String expectedState = "state";
        String expectedUrl = "http://token-uri" +
                "?grant_type=grant-type" +
                "&client_id=client-id" +
                "&client_secret=client-secret" +
                "&code=" + expectedCode +
                "&state=" + expectedState;
        String actualUrl = naverURLBuilder.getToken(expectedCode, expectedState);

        assertThat(actualUrl).isEqualTo(expectedUrl);
    }

    @Test
    void getUserInfo() {
        String expectedUrl = "http://user-info-uri";
        String actualUrl = naverURLBuilder.getUserInfo();

        assertThat(actualUrl).isEqualTo(expectedUrl);
    }
}