package com.moemoe.core.service.provider;

import com.moemoe.client.aws.AwsS3Client;
import com.moemoe.client.aws.dto.S3ObjectStream;
import com.moemoe.core.response.GetProductImageServiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductImageProvider {
    private final AwsS3Client awsS3Client;

    public GetProductImageServiceResponse provide(String imageKey) {
        if (!StringUtils.hasText(imageKey)) {
            return loadFallback();
        }

        try {
            S3ObjectStream objectStream = awsS3Client.getObjectStream(imageKey);
            return GetProductImageServiceResponse.ofStream(objectStream.stream(), objectStream.contentType(), objectStream.contentLength(), false);
        } catch (Exception e) {
            log.warn("Failed to load thumbnail. key={} cause={}", imageKey, e.toString());
            return loadFallback();
        }
    }

    private GetProductImageServiceResponse loadFallback() {
        // fallback key는 아직 정해진게 없으므로 빈 문자열
        String fallbackKey = "";
        try {
            S3ObjectStream objectStream = awsS3Client.getObjectStream(fallbackKey);
            return GetProductImageServiceResponse.ofStream(objectStream.stream(), objectStream.contentType(), objectStream.contentLength(), true);
        } catch (Exception e) {
            log.error("Fallback also failed. key={} cause={}", fallbackKey, e.toString());
            return GetProductImageServiceResponse.tinyGif();
        }
    }
}
