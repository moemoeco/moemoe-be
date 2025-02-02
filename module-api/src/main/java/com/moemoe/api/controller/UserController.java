package com.moemoe.api.controller;


import com.moemoe.core.request.RefreshAccessTokenRequest;
import com.moemoe.core.response.LoginTokenResponse;
import com.moemoe.core.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/refresh")
    public LoginTokenResponse refresh(
            @Valid @RequestBody RefreshAccessTokenRequest request
    ) {
        return userService.refresh(request);
    }
}
