package com.moemoe.client.http.dto;

public interface TokenResponse {
    String tokenType();

    String accessToken();

    String refreshToken();

    int expiresIn();

    String authorizationToken();
}
