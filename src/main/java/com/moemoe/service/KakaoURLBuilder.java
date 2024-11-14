package com.moemoe.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class KakaoURLBuilder {
    @Value("${spring.security.oauth2.client.provider.kakao.authorization-uri}")
    private String authorizationUri;
    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    private String tokenUri;
    @Value("${spring.security.oauth2.client.provider.kakao.user-info-uri}")
    private String userInfoUri;
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;
    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String redirectUri;
    @Value("${spring.security.oauth2.client.registration.kakao.scope}")
    private String scopes;


    public String authorize(String state) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put("response_type", List.of("code"));
        params.put("client_id", List.of(clientId));
        params.put("redirect_uri", List.of(redirectUri));
        params.put("state", List.of(state));
        params.put("scope", List.of(scopes));

        return UriComponentsBuilder.fromHttpUrl(authorizationUri)
                .queryParams(params)
                .build()
                .toString();
    }

    public String getToken(String code, String state) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put("grant_type", List.of("authorization_code"));
        params.put("client_id", List.of(clientId));
        params.put("redirect_uri", List.of(redirectUri));
        params.put("code", List.of(code));
        params.put("state", List.of(state));
        params.put("client_secret", List.of(clientSecret));

        return UriComponentsBuilder.fromHttpUrl(tokenUri)
                .queryParams(params)
                .build()
                .toString();
    }

    public String getUserInfo() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.put("secure_resource", List.of("true"));
        return UriComponentsBuilder.fromHttpUrl(userInfoUri)
                .queryParams(params)
                .build()
                .toString();
    }
}
