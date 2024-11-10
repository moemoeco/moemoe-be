package com.moemoe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfoResponse(
        long id,
        @JsonProperty("connected_at") String connectedAt,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    public record KakaoAccount(
            Profile profile,
            String name,
            @JsonProperty("is_email_valid") boolean isEmailValid,
            @JsonProperty("is_email_verified") boolean isEmailVerified,
            String email,
            String birthyear,
            String birthday,
            @JsonProperty("birthday_type") String birthdayType,
            String gender
    ) {
    }

    public record Profile(
            String nickname,
            @JsonProperty("thumbnail_image_url") String thumbnailImageUrl,
            @JsonProperty("profile_image_url") String profileImageUrl,
            @JsonProperty("is_default_image") boolean isDefaultImage,
            @JsonProperty("is_default_nickname") boolean isDefaultNickname
    ) {
    }
}
