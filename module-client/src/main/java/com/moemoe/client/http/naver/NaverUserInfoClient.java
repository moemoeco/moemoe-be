package com.moemoe.client.http.naver;

import com.moemoe.client.http.dto.naver.NaverUserInfoResponse;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.net.URI;

@HttpExchange("https://openapi.naver.com/v1/nid/me")
public interface NaverUserInfoClient {
    @PostExchange
    NaverUserInfoResponse getUserInfo(URI uri, @RequestHeader("Authorization") String authorization);
}
