package com.moemoe.dto.naver;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moemoe.dto.TokenResponse;

public record NaverTokenResponse(
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("error") String errorMsg,
        @JsonProperty("error_description") String errorDescription
) implements TokenResponse {
    @Override
    public String authorizationToken() {
        return this.tokenType + " " + this.accessToken;
    }
}
