package com.moemoe.controller;


import com.moemoe.constant.OAuthPlatform;
import com.moemoe.dto.LoginTokenResponse;
import com.moemoe.service.KakaoOAuthService;
import com.moemoe.service.NaverOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth")
@CrossOrigin(origins = "*", methods = RequestMethod.GET)
public class OAuthController {
    private final KakaoOAuthService kakaoOAuthService;
    private final NaverOAuthService naverOAuthService;

    @GetMapping("/{platformType}/login-page")
    public String loginPage(
            @PathVariable OAuthPlatform platformType) {
        log.info("Login page");
        if (platformType.equals(OAuthPlatform.KAKAO)) {
            return kakaoOAuthService.authorize();
        } else if (platformType.equals(OAuthPlatform.NAVER)) {
            return naverOAuthService.authorize("");
        } else {
            throw new IllegalArgumentException("Unknown platform type: " + platformType);
        }
    }

    @GetMapping("/login")
    public LoginTokenResponse login(
            @RequestParam("code") String code) {
        log.info("Login");
        return kakaoOAuthService.login(code);
    }
}
