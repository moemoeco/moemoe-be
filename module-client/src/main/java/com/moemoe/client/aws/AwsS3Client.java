package com.moemoe.client.aws;

import com.moemoe.client.aws.dto.AwsProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwsS3Client {
    private final AwsProperty awsProperty;

    public S3Client getS3Client(Region region, AwsCredentials awsCredentials) {
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .region(awsProperty.getRegion())
                .build();
    }

    public void upload(String s3ObjectKey, S3Client s3Client, MultipartFile multipartFile) {
        PutObjectRequest.builder()
//                .bucket()
                .build();
    }
}
