package com.moemoe.core.response;

import lombok.Builder;

@Builder
public record LoginTokenResponse(
        String accessToken,
        String refreshToken
) {
}
