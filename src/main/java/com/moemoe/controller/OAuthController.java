package com.moemoe.controller;


import com.moemoe.service.KakaoOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController("/oauth")
@RequiredArgsConstructor
public class OAuthController {
    private final KakaoOAuthService kakaoOAuthService;

    @GetMapping("/login-page")
    public String loginPage() {
        return kakaoOAuthService.authorize();
    }
}
