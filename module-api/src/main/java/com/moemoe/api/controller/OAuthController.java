package com.moemoe.api.controller;


import com.moemoe.api.constant.OAuthPlatform;
import com.moemoe.core.response.AuthorizationResponse;
import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.oauth.KakaoOAuthService;
import com.moemoe.core.service.oauth.NaverOAuthService;
import com.moemoe.core.service.oauth.OAuthTemplate;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
    public void login(
            @PathVariable OAuthPlatform platformType,
            @RequestParam("code") String code,
            @RequestParam(value = "state") String state,
            HttpServletResponse response) {
        OAuthTemplate oAuthTemplate = getOAuthTemplate(platformType);
        LoginTokenResponse tokenResponse = oAuthTemplate.login(code, state);

        try {
            String refreshToken = tokenResponse.refreshToken();
            String accessToken = tokenResponse.accessToken();
            Cookie accessTokenCookie = getCookie("accessToken", accessToken, 60 * 60);
            Cookie refreshTokenCookie = getCookie("refreshToken", refreshToken, 60 * 60 * 24 * 7);
            response.addCookie(accessTokenCookie);
            response.addCookie(refreshTokenCookie);
            response.setStatus(HttpStatus.OK.value());
            response.sendRedirect("http://localhost:8081?redirectedFromSocialLogin=true");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Cookie getCookie(String key, String value, int expiry) {
        Cookie cookie = new Cookie(key, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(expiry);
        return cookie;
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
