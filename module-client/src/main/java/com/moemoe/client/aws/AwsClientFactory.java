package com.moemoe.client.aws;

import com.moemoe.client.aws.property.AwsProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Service
@RequiredArgsConstructor
public class AwsClientFactory {
    private final AwsProperty awsProperty;

    public S3Presigner getS3Presigner() {
        AwsBasicCredentials awsBasicCredentials = getAwsBasicCredentials();
        return S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .region(awsProperty.getRegion())
                .build();
    }

    public S3Client getS3Client() {
        AwsBasicCredentials awsBasicCredentials = getAwsBasicCredentials();
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .region(awsProperty.getRegion())
                .build();
    }

    private AwsBasicCredentials getAwsBasicCredentials() {
        return AwsBasicCredentials.create(
                awsProperty.getAccessKey(),
                awsProperty.getSecretKey()
        );
    }
}
