package com.moemoe.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class KakaoURLBuilderTest {
    @Autowired
    private KakaoURLBuilder kakaoURLBuilder;

    @Test
    void authorize() {
        String expectedUrl = "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=client-id&redirect_uri=redirect-uri&scope=profile_nickname,name";
        String actualUrl = kakaoURLBuilder.authorize();

        assertThat(actualUrl)
                .isEqualTo(expectedUrl);
    }
}