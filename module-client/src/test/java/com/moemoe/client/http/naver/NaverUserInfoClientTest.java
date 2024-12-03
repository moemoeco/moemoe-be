package com.moemoe.client.http.naver;

import com.moemoe.client.http.AbstractMockWebServer;
import com.moemoe.client.http.dto.naver.NaverUserInfoResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NaverUserInfoClientTest extends AbstractMockWebServer {
    @Autowired
    private NaverUserInfoClient naverUserInfoClient;

    @Test
    void getUserInfo() {
        // given
        String expectedUri = "/v1/nid/me/user";
        String expectedAuthToken = "auth token";
        ExpectedNaverUserInfoResponse.ExpectedNaverAccount naverAccount = ExpectedNaverUserInfoResponse.ExpectedNaverAccount.builder()
                .id("sampleId")
                .profile_image("sampleProfileImages")
                .gender("male")
                .email("sample@example.com")
                .mobile("010-1234-5678")
                .mobile_e164("+821012345678")
                .name("Sample Name")
                .birthday("12-25")
                .birthyear("1990")
                .build();
        ExpectedNaverUserInfoResponse expectedResponse = ExpectedNaverUserInfoResponse.builder()
                .resultcode("00")
                .message("Success")
                .response(naverAccount)
                .build();

        //  Content type 'application/x-www-form-urlencoded' not supported for bodyType=
        mockResponse(expectedResponse, Map.of("Content-Type", MediaType.APPLICATION_JSON, "Authorization", expectedAuthToken));

        // when
        NaverUserInfoResponse actualUserInfo = naverUserInfoClient.getUserInfo(getUri(expectedUri), expectedAuthToken);

        // then
        assertThat(actualUserInfo.resultCode()).isEqualTo(expectedResponse.getResultcode());
        assertThat(actualUserInfo.message()).isEqualTo(expectedResponse.getMessage());

        NaverUserInfoResponse.NaverAccount actualAccount = actualUserInfo.naverAccount();
        ExpectedNaverUserInfoResponse.ExpectedNaverAccount expectedAccount = expectedResponse.getResponse();
        assertThat(actualAccount.id()).isEqualTo(expectedAccount.getId());
        assertThat(actualAccount.profileImageUrl()).isEqualTo(expectedAccount.getProfile_image());
        assertThat(actualAccount.gender()).isEqualTo(expectedAccount.getGender());
        assertThat(actualAccount.email()).isEqualTo(expectedAccount.getEmail());
        assertThat(actualAccount.mobile()).isEqualTo(expectedAccount.getMobile());
        assertThat(actualAccount.mobileE164()).isEqualTo(expectedAccount.getMobile_e164());
        assertThat(actualAccount.name()).isEqualTo(expectedAccount.getName());
        assertThat(actualAccount.birthday()).isEqualTo(expectedAccount.getBirthday());
        assertThat(actualAccount.birthyear()).isEqualTo(expectedAccount.getBirthyear());


    }

    @Getter
    @Builder
    @AllArgsConstructor
    static class ExpectedNaverUserInfoResponse {
        private String resultcode;
        private String message;
        private ExpectedNaverAccount response;

        @Getter
        @Builder
        @AllArgsConstructor
        static class ExpectedNaverAccount {
            private String id;
            private String profile_image;
            private String gender;
            private String email;
            private String mobile;
            private String mobile_e164;
            private String name;
            private String birthday;
            private String birthyear;
        }
    }
}