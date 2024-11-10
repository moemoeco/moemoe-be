package com.moemoe.dto;

import lombok.Builder;

@Builder
public record LoginTokenResponse(
        String accessToken,
        String refreshToken
) {
}
