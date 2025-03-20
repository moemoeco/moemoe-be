package com.moemoe.api.controller;


import com.moemoe.api.constant.OAuthPlatform;
import com.moemoe.core.request.OAuthLoginRequest;
import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.oauth.KakaoOAuthService;
import com.moemoe.core.service.oauth.NaverOAuthService;
import com.moemoe.core.service.oauth.OAuthTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/oauth")
public class OAuthController {
    private final Map<OAuthPlatform, OAuthTemplate> oAuthTemplateMap;

    public OAuthController(KakaoOAuthService kakaoOAuthService,
                           NaverOAuthService naverOAuthService) {
        this.oAuthTemplateMap = Map.of(
                OAuthPlatform.NAVER, naverOAuthService,
                OAuthPlatform.KAKAO, kakaoOAuthService
        );
    }

    @PostMapping("/{platformType}/login")
    public LoginTokenResponse login(
            @PathVariable(value = "platformType") OAuthPlatform platformType,
            @RequestBody OAuthLoginRequest request
    ) {
        OAuthTemplate oAuthTemplate = getOAuthTemplate(platformType);
        return oAuthTemplate.login(request);
    }

    private OAuthTemplate getOAuthTemplate(OAuthPlatform platformType) {
        OAuthTemplate oAuthTemplate;
        try {
            oAuthTemplate = oAuthTemplateMap.get(platformType);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Unknown platform type: " + platformType);
        }
        return oAuthTemplate;
    }
}
