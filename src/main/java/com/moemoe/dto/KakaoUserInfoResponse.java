package com.moemoe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class KakaoUserInfoResponse {
    private long id;
    @JsonProperty("connected_at")
    private String connectedAt;
    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    public static class KakaoAccount {
        private Profile profile;
        private String name;
        @JsonProperty("is_email_valid")
        private boolean isEmailValid;
        @JsonProperty("is_email_verified")
        private boolean isEmailVerified;
        private String email;
        @JsonProperty("age_range")
        private String ageRange;
        private String birthyear;
        private String birthday;
        @JsonProperty("birthday_type")
        private String birthdayType;
        private String gender;
    }

    @Getter
    public static class Profile {
        private String nickname;
        @JsonProperty("thumbnail_image_url")
        private String thumbnailImageUrl;
        @JsonProperty("profile_image_url")
        private String profileImageUrl;
        @JsonProperty("is_default_image")
        private boolean isDefaultImage;
        @JsonProperty("is_default_nickname")
        private boolean isDefaultNickname;
    }
}
