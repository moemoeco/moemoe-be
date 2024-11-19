package com.moemoe.http.client.naver;

import com.moemoe.dto.naver.NaverTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.net.URI;

@HttpExchange("https://nid.naver.com/oauth2.0/token")
public interface NaverTokenClient {
    @PostExchange(contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    NaverTokenResponse getToken(URI uri);
}
