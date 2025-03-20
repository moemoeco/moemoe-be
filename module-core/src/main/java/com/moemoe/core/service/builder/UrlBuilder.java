package com.moemoe.core.service.builder;

public interface UrlBuilder {
    String getTokenUrl(String code, String state);

    String getUserInfoUrl();
}
