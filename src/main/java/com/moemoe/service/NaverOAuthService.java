package com.moemoe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NaverOAuthService {
    private final NaverURLBuilder naverURLBuilder;

    public String authorize(String state) {
        return naverURLBuilder.authorize(state);
    }
}
