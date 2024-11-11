package com.moemoe.constant;


import org.springframework.core.convert.converter.Converter;

public class OAuthPlatformConverter implements Converter<String, OAuthPlatform> {
    @Override
    public OAuthPlatform convert(String source) {
        return OAuthPlatform.valueOf(source.toUpperCase());
    }
}
