package com.moemoe.client.http.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moemoe.client.http.dto.TokenResponse;

public record KakaoTokenResponse(
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("id_token") String idToken,
        @JsonProperty("refresh_token_expires_in") int refreshTokenExpiresIn
) implements TokenResponse {
    @Override
    public String authorizationToken() {
        return this.tokenType + " " + this.accessToken;
    }
}
