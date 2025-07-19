package com.moemoe.client.aws;

import com.fasterxml.uuid.Generators;
import com.moemoe.client.aws.dto.AwsProperty;
import com.moemoe.client.aws.dto.PresignedFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwsS3PresignedClient {
    private final AwsClientFactory awsClientFactory;
    private final AwsProperty awsProperty;

    public PresignedFile generatePresignedUrl(String fileName, String contentType) {
        try (S3Presigner s3Presigner = awsClientFactory.getS3Presigner()) {
            String key = getKey(fileName);
            log.debug("Generating presigned URL for file: {}, contentType: {}, key: {}", fileName, contentType, key);

            PutObjectPresignRequest putObjectPresignRequest = getPutObjectPresignRequest(contentType, key);
            PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(putObjectPresignRequest);
            String uploadUrl = presignedPutObjectRequest.url().toString();

            log.debug("Successfully generated presigned upload URL for key: {}", key);

            return PresignedFile.of(fileName, uploadUrl, key);

        } catch (Exception e) {
            log.error("Failed to generate presigned URL. fileName: {}, contentType: {}, error: {}",
                    fileName, contentType, e.getMessage(), e);
            throw new IllegalArgumentException("Failed to generate presigned URL for file: " + fileName, e);
        }
    }

    private PutObjectPresignRequest getPutObjectPresignRequest(String contentType, String key) {
        return PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(builder -> builder
                        .bucket(awsProperty.getBucketName())
                        .key(key)
                        .contentType(contentType)
                        .build())
                .build();
    }

    private String getKey(String fileName) {
        return "products/images/" + Generators.timeBasedEpochGenerator().generate() + "_" + fileName;
    }
}
