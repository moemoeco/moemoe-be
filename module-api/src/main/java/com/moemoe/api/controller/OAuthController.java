package com.moemoe.api.controller;


import com.moemoe.api.constant.OAuthPlatform;
import com.moemoe.core.dto.AuthorizationResponse;
import com.moemoe.core.dto.LoginTokenResponse;
import com.moemoe.core.service.oauth.KakaoOAuthService;
import com.moemoe.core.service.oauth.NaverOAuthService;
import com.moemoe.core.service.oauth.OAuthTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/oauth")
@CrossOrigin(origins = "*", methods = RequestMethod.GET)
public class OAuthController {
    private final Map<OAuthPlatform, OAuthTemplate> oAuthTemplateMap;

    public OAuthController(KakaoOAuthService kakaoOAuthService,
                           NaverOAuthService naverOAuthService) {
        this.oAuthTemplateMap = Map.of(
                OAuthPlatform.NAVER, naverOAuthService,
                OAuthPlatform.KAKAO, kakaoOAuthService
        );
    }

    @GetMapping("/{platformType}/login-page")
    public AuthorizationResponse loginPage(
            @PathVariable OAuthPlatform platformType) {
        log.info("Login page");
        OAuthTemplate oAuthTemplate = getOAuthTemplate(platformType);
        return oAuthTemplate.authorize("");
    }

    @GetMapping("/{platformType}/login")
    public LoginTokenResponse login(
            @PathVariable OAuthPlatform platformType,
            @RequestParam("code") String code,
            @RequestParam(value = "state") String state) {
        OAuthTemplate oAuthTemplate = getOAuthTemplate(platformType);
        return oAuthTemplate.login(code, state);
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
