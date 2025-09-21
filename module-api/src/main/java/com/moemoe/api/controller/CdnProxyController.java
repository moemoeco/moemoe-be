package com.moemoe.api.controller;


import com.moemoe.core.response.GetProductImageServiceResponse;
import com.moemoe.core.service.provider.ProductImageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/cdn")
@RequiredArgsConstructor
public class CdnProxyController {
    private final ProductImageProvider productImageProvider;

    @GetMapping("/product-images/{*imageKey}")
    public ResponseEntity<StreamingResponseBody> getProductImages(
            @PathVariable String imageKey
    ) {
        String normalizedKey = getNormalizedKey(imageKey);

        GetProductImageServiceResponse serviceResponse = productImageProvider.provide(normalizedKey);
        final CacheControl cacheControl = getProductImageCacheControl(serviceResponse);

        if (serviceResponse.isInMemory()) {
            byte[] inline = serviceResponse.inline();
            return ResponseEntity.ok()
                    .cacheControl(cacheControl)
                    .contentType(MediaType.parseMediaType(serviceResponse.contentType()))
                    .contentLength(inline.length)
                    .body(outputStream -> outputStream.write(inline));
        }

        StreamingResponseBody body = outputStream -> {
            try (serviceResponse; InputStream inputStream = serviceResponse.inputStream()) {
                inputStream.transferTo(outputStream);
            } catch (IOException e) {
                log.warn("Stream write failed for key={} : {}", imageKey, e.toString());
            }
        };
        return ResponseEntity.ok()
                .cacheControl(cacheControl)
                .contentType(MediaType.parseMediaType(serviceResponse.contentType()))
                .contentLength(serviceResponse.contentLength())
                .body(body);
    }

    private CacheControl getProductImageCacheControl(GetProductImageServiceResponse serviceResponse) {
        return serviceResponse.fallback()
                ? CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic()
                : CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();
    }

    private String getNormalizedKey(String imageKey) {
        String normalizedKey = imageKey != null && imageKey.startsWith("/") ? imageKey.substring(1) : imageKey;
        return normalizedKey;
    }
}
