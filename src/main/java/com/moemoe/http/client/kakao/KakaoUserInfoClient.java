package com.moemoe.http.client.kakao;

import com.moemoe.dto.KakaoUserInfoResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.net.URI;

@HttpExchange("https://kapi.kakao.com/v2/user/me")
public interface KakaoUserInfoClient {
    @PostExchange(contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    KakaoUserInfoResponse getUserInfo(URI uri, @RequestHeader("Authorization") String authorization);
}
