package com.moemoe.core.service.resolver;

import com.moemoe.core.property.CdnProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductImageKeyResolver {
    private final CdnProperty cdnProperty;

    public String resolve(String imageKey) {
        String proxy = cdnProperty.proxy();
        log.debug("Resolving product image URL: proxy='{}', rawKey='{}'", proxy, imageKey);
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(proxy)
                .pathSegment("cdn", "product-images");
        for (String seg : imageKey.split("/")) {
            if (StringUtils.hasText(seg)) {
                uriComponentsBuilder.pathSegment(seg);
            }
        }
        String url = uriComponentsBuilder
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
        log.info("Resolved product image URL -> {}", url);
        return url;
    }
}
