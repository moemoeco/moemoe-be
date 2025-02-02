package com.moemoe.core.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

@Getter
public class RefreshAccessTokenRequest {
    @NotEmpty
    private String refreshToken;

}
