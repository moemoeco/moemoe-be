package com.moemoe.core.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cdn")
public record CdnProperty(
        String proxy
) {
}
