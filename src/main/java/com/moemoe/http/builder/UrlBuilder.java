package com.moemoe.http.builder;

public interface UrlBuilder {
    String getAuthorizeUrl(String state);

    String getTokenUrl(String code, String state);

    String getUserInfoUrl();
}
