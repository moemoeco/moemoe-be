package com.moemoe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NaverUserInfoResponse(
        @JsonProperty("resultcode") String resultCode,
        String message,
        NaverAccount response
) {
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
