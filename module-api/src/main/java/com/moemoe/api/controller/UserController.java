package com.moemoe.api.controller;


import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/refresh")
    public LoginTokenResponse refresh(
            @RequestHeader(name = "Authorization") String authorization
    ) {
        String refreshToken = authorization.split(" ")[1];
        return userService.refresh(refreshToken);
    }
}
