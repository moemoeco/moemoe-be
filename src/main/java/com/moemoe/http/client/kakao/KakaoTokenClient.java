package com.moemoe.http.client.kakao;

import com.moemoe.dto.kakao.KakaoTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.net.URI;

@HttpExchange("https://kauth.kakao.com/oauth/token")
public interface KakaoTokenClient {
    @PostExchange(contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    KakaoTokenResponse getToken(URI uri);
}
