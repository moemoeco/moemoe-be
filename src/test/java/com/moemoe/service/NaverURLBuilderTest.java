package com.moemoe.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.stream;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.security.oauth2.client.provider.naver.authorization-uri=http://authorization-uri",
        "spring.security.oauth2.client.provider.naver.token-uri=http://token-uri",
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
}