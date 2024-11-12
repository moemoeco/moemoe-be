package com.moemoe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NaverTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("error") String errorMsg,
        @JsonProperty("error_description") String errorDescription
) {
    public String getAuthorizationToken() {
        return this.tokenType + " " + this.accessToken;
    }
}
