package com.moemoe.http.builder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {KakaoUrlBuilder.class, NaverUrlBuilder.class})
@TestPropertySource(properties = {
        // kakao
        "spring.security.oauth2.client.provider.kakao.authorization-uri=http://authorization-uri",
        "spring.security.oauth2.client.provider.kakao.token-uri=http://token-uri",
        "spring.security.oauth2.client.provider.kakao.user-info-uri=http://user-info-uri",
        "spring.security.oauth2.client.registration.kakao.client-id=client-id",
        "spring.security.oauth2.client.registration.kakao.client-secret=client-secret",
        "spring.security.oauth2.client.registration.kakao.redirect-uri=redirect-uri",
        "spring.security.oauth2.client.registration.kakao.scope=profile_nickname,name",
        // naver
        "spring.security.oauth2.client.provider.naver.authorization-uri=http://authorization-uri",
        "spring.security.oauth2.client.provider.naver.token-uri=http://token-uri",
        "spring.security.oauth2.client.provider.naver.user-info-uri=http://user-info-uri",
        "spring.security.oauth2.client.registration.naver.client-id=client-id",
        "spring.security.oauth2.client.registration.naver.client-secret=client-secret",
        "spring.security.oauth2.client.registration.naver.redirect-uri=redirect-uri",
        "spring.security.oauth2.client.registration.naver.authorization-grant-type=grant-type"
})
class UrlBuilderTest {
    @Nested
    class KakaoUrlBuilderTest {
        @Autowired
        UrlBuilder kakaoUrlBuilder;

        @Test
        void getAuthorizeUrl() {
            String expectedUrl = "http://authorization-uri" +
                    "?response_type=code" +
                    "&client_id=client-id" +
                    "&redirect_uri=redirect-uri" +
                    "&state=state" +
                    "&scope=profile_nickname,name";
            String actualUrl = kakaoUrlBuilder.getAuthorizeUrl("state");

            assertThat(actualUrl)
                    .isEqualTo(expectedUrl);
        }

        @Test
        void getTokenUrl() {
            String expectedUrl = "http://token-uri" +
                    "?grant_type=authorization_code" +
                    "&client_id=client-id" +
                    "&redirect_uri=redirect-uri" +
                    "&code=test" +
                    "&state=state" +
                    "&client_secret=client-secret";

            String actualUrl = kakaoUrlBuilder.getTokenUrl("test", "state");

            assertThat(actualUrl)
                    .isEqualTo(expectedUrl);
        }

        @Test
        void getUserInfoUrl() {
            String expectedUrl = "http://user-info-uri" +
                    "?secure_resource=true";

            String actualUrl = kakaoUrlBuilder.getUserInfoUrl();

            assertThat(actualUrl)
                    .isEqualTo(expectedUrl);
        }
    }

    @Nested
    class NaverUrlBuilderTest {
        @Autowired
        UrlBuilder naverUrlBuilder;


        @Test
        void getAuthorizeUrl() {
            String expectedState = "test";
            String expectedUrl = "http://authorization-uri" +
                    "?response_type=code" +
                    "&client_id=client-id" +
                    "&redirect_uri=redirect-uri" +
                    "&state=" + expectedState;
            String actualUrl = naverUrlBuilder.getAuthorizeUrl(expectedState);

            assertThat(actualUrl)
                    .isEqualTo(expectedUrl);
        }

        @Test
        void getTokenUrl() {
            String expectedCode = "code";
            String expectedState = "state";
            String expectedUrl = "http://token-uri" +
                    "?grant_type=grant-type" +
                    "&client_id=client-id" +
                    "&client_secret=client-secret" +
                    "&code=" + expectedCode +
                    "&state=" + expectedState;
            String actualUrl = naverUrlBuilder.getTokenUrl(expectedCode, expectedState);

            assertThat(actualUrl).isEqualTo(expectedUrl);
        }

        @Test
        void getUserInfoUrl() {
            String expectedUrl = "http://user-info-uri";
            String actualUrl = naverUrlBuilder.getUserInfoUrl();

            assertThat(actualUrl).isEqualTo(expectedUrl);
        }
    }
}