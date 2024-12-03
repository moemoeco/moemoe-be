package com.moemoe.client.http.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moemoe.client.http.dto.UserInfoResponse;

public record KakaoUserInfoResponse(
        String id,
        @JsonProperty("connected_at") String connectedAt,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) implements UserInfoResponse {
    public record KakaoAccount(
            Profile profile,
            String name,
            String email,
            String birthyear,
            String birthday,
            @JsonProperty("birthday_type") String birthdayType,
            String gender
    ) {

        public record Profile(
                String nickname,
                @JsonProperty("profile_image_url") String profileImageUrl
        ) {
        }
    }
}
