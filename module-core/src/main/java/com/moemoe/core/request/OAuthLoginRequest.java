package com.moemoe.core.request;

import org.springframework.security.oauth2.core.OAuth2AccessToken;

public record OAuthLoginRequest(
        String tokenType,
        String token
) {
    public String accessToken() {
        if (OAuth2AccessToken.TokenType.BEARER.getValue().equals(tokenType)) {
            return this.tokenType + " " + this.token;
        }
        throw new IllegalArgumentException("Invalid Token Type " + tokenType);
    }
}
