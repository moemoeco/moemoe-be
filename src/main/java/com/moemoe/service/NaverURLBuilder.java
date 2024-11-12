package com.moemoe.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class NaverURLBuilder {
    @Value("${spring.security.oauth2.client.provider.naver.authorization-uri}")
    private String authorizationUri;
    @Value("${spring.security.oauth2.client.provider.naver.token-uri}")
    private String tokenUri;
    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String clientSecret;
    @Value("${spring.security.oauth2.client.registration.naver.redirect-uri}")
    private String redirectUri;
    @Value("${spring.security.oauth2.client.registration.naver.authorization-grant-type}")
    private String grantType;

    public String authorize(String state) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put("response_type", List.of("code"));
        params.put("client_id", List.of(clientId));
        params.put("redirect_uri", List.of(redirectUri));
        params.put("state", List.of(state));

        return UriComponentsBuilder.fromHttpUrl(authorizationUri)
                .queryParams(params)
                .build()
                .toString();
    }

    public String getToken(String code, String state) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put("grant_type", List.of(grantType));
        params.put("client_id", List.of(clientId));
        params.put("client_secret", List.of(clientSecret));
        params.put("code", List.of(code));
        params.put("state", List.of(state));

        return UriComponentsBuilder.fromHttpUrl(tokenUri)
                .queryParams(params)
                .build()
                .toString();
    }
}
