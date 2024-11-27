package com.moemoe.client.http.kakao;

import com.moemoe.client.http.AbstractMockWebServer;
import com.moemoe.client.http.dto.kakao.KakaoUserInfoResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoUserInfoClientTest extends AbstractMockWebServer {
    @Autowired
    private KakaoUserInfoClient kakaoUserInfoClient;

    @Test
    void getUserInfo() {
        // given
        String expectedUrl = "/v2/user/me";
        String authorization = "Bearer sampleAccessToken123";

        // create expected builder
        ExpectedKakaoUserInfoResponse expected = ExpectedKakaoUserInfoResponse.builder()
                .id("123456789")
                .connected_at("2024-11-27T12:00:00Z")
                .kakao_account(ExpectedKakaoUserInfoResponse.ExpectedKakaoAccount.builder()
                        .profile(ExpectedKakaoUserInfoResponse.ExpectedProfile.builder()
                                .nickname("John Doe")
                                .profile_image_url("https://sample.com/profile.jpg")
                                .build())
                        .name("John Doe")
                        .email("johndoe@example.com")
                        .birthyear("1990")
                        .birthday("0101")
                        .birthday_type("SOLAR")
                        .gender("male")
                        .build())
                .build();
        mockResponse(expected, Map.of("Content-Type", MediaType.APPLICATION_JSON, "Authorization", authorization));


        // when
        KakaoUserInfoResponse actual = kakaoUserInfoClient.getUserInfo(getUri(expectedUrl), authorization);

        // then
        // assert actual expected
        assertThat(actual.id()).isEqualTo(expected.getId());
        assertThat(actual.connectedAt()).isEqualTo(expected.getConnected_at());

        // Assertions for KakaoAccount fields
        KakaoUserInfoResponse.KakaoAccount actualAccount = actual.kakaoAccount();
        ExpectedKakaoUserInfoResponse.ExpectedKakaoAccount expectedAccount = expected.getKakao_account();

        assertThat(actualAccount.name()).isEqualTo(expectedAccount.getName());
        assertThat(actualAccount.email()).isEqualTo(expectedAccount.getEmail());
        assertThat(actualAccount.birthyear()).isEqualTo(expectedAccount.getBirthyear());
        assertThat(actualAccount.birthday()).isEqualTo(expectedAccount.getBirthday());
        assertThat(actualAccount.birthdayType()).isEqualTo(expectedAccount.getBirthday_type());
        assertThat(actualAccount.gender()).isEqualTo(expectedAccount.getGender());

        // Assertions for Profile fields
        KakaoUserInfoResponse.KakaoAccount.Profile actualProfile = actualAccount.profile();
        ExpectedKakaoUserInfoResponse.ExpectedProfile expectedProfile = expectedAccount.getProfile();

        assertThat(actualProfile.nickname()).isEqualTo(expectedProfile.getNickname());
        assertThat(actualProfile.profileImageUrl()).isEqualTo(expectedProfile.getProfile_image_url());
    }


    @Getter
    @Builder
    @AllArgsConstructor
    static class ExpectedKakaoUserInfoResponse {
        private String id;
        private String connected_at;
        private ExpectedKakaoAccount kakao_account;

        @Getter
        @Builder
        @AllArgsConstructor
        static class ExpectedKakaoAccount {
            private ExpectedProfile profile;
            private String name;
            private String email;
            private String birthyear;
            private String birthday;
            private String birthday_type;
            private String gender;
        }

        @Getter
        @Builder
        @AllArgsConstructor
        static class ExpectedProfile {
            private String nickname;
            private String profile_image_url;
        }
    }

}