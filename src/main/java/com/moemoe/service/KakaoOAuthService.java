package com.moemoe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {
    private final KakaoURLBuilder kakaoURLBuilder;

    public String authorize() {
        return kakaoURLBuilder.authorize();
    }
}
