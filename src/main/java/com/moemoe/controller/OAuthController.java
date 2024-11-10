package com.moemoe.controller;


import com.moemoe.dto.LoginTokenResponse;
import com.moemoe.service.KakaoOAuthService;
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

    @GetMapping("/login-page")
    public String loginPage() {
        log.info("Login page");
        return kakaoOAuthService.authorize();
    }

    @GetMapping("/login")
    public LoginTokenResponse login(
            @RequestParam("code") String code) {
        log.info("Login");
        return kakaoOAuthService.login(code);
    }
}
