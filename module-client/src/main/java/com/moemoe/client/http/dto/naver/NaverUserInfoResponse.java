package com.moemoe.client.http.dto.naver;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moemoe.client.http.dto.UserInfoResponse;

public record NaverUserInfoResponse(
        @JsonProperty("resultcode") String resultCode,
        String message,
        @JsonProperty("response") NaverAccount naverAccount
) implements UserInfoResponse {
    public record NaverAccount(
            String id,
            @JsonProperty("profile_image") String profileImageUrl,
            String gender,
            String email,
            String mobile,
            @JsonProperty("mobile_e164") String mobileE164,
            String name,
            String birthday,
            String birthyear
    ) {
    }
}
