package com.moemoe.client.aws.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import software.amazon.awssdk.regions.Region;

@Getter
@ConfigurationProperties(prefix = "aws")
public class AwsProperty {
    private final Credentials credentials;
    private final Region region;
    private final S3 s3;

    public AwsProperty(Credentials credentials, Region region, S3 s3) {
        if (s3 == null) {
            throw new IllegalArgumentException("AWS S3 configuration is required");
        }
        if (credentials == null) {
            throw new IllegalArgumentException("AWS credentials configuration is required");
        }

        this.credentials = credentials;
        this.region = region;
        this.s3 = s3;
    }

    public String getBucketName() {
        return s3.getBucketName();
    }

    public String getAccessKey() {
        return credentials.getAccessKey();
    }

    public String getSecretKey() {
        return credentials.getSecretKey();
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    public static class S3 {
        private final String bucketName;
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Credentials {
        private final String accessKey;
        private final String secretKey;
    }
}
