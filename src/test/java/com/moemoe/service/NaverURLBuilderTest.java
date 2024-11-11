package com.moemoe.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.security.oauth2.client.provider.naver.authorization-uri=https://nid.naver.com/oauth2.0/authorize",
        "spring.security.oauth2.client.registration.naver.client-id=client-id",
        "spring.security.oauth2.client.registration.naver.redirect-uri=redirect-uri"
})
class NaverURLBuilderTest {
    @Autowired
    private NaverURLBuilder naverURLBuilder;

    @Test
    void authorize() {
        String expectedState = "test";
        String expectedUrl = "https://nid.naver.com/oauth2.0/authorize" +
                "?response_type=code" +
                "&client_id=client-id" +
                "&redirect_uri=redirect-uri" +
                "&state=" + expectedState;
        String actualUrl = naverURLBuilder.authorize(expectedState);

        assertThat(actualUrl)
                .isEqualTo(expectedUrl);
    }
}