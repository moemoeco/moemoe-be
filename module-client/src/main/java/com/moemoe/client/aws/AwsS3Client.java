package com.moemoe.client.aws;

import com.moemoe.client.aws.dto.AwsProperty;
import com.moemoe.client.exception.ClientRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwsS3Client {
    private final AwsProperty awsProperty;
    private final AwsClientFactory awsClientFactory;

    public String upload(String s3ObjectKey, MultipartFile multipartFile) {
        try (S3Client s3Client = awsClientFactory.getS3Client();
             InputStream inputStream = multipartFile.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(awsProperty.getBucketName())
                    .key(s3ObjectKey)
                    .contentType(multipartFile.getContentType())
                    .build();

            PutObjectResponse putObjectResponse = s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(inputStream, multipartFile.getSize())
            );

            SdkHttpResponse sdkHttpResponse = putObjectResponse.sdkHttpResponse();
            if (sdkHttpResponse.isSuccessful()) {
                log.info("S3 upload succeeded: S3 Object Key: {} , Status Code : {}", s3ObjectKey, sdkHttpResponse.statusCode());
                return s3ObjectKey;
            } else {
                throw new IllegalArgumentException("S3 upload failed: Status code : " + sdkHttpResponse.statusCode());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("An error occurred during file upload: " + e.getMessage(), e);
        }
    }

    public void delete(List<String> s3ObjectKeyList) {
        if (ObjectUtils.isEmpty(s3ObjectKeyList)) {
            throw new IllegalArgumentException("삭제할 객체 키 목록이 비어 있습니다.");
        }

        DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                .bucket(awsProperty.getBucketName())
                .delete(builder -> builder.objects(s3ObjectKeyList.stream()
                        .map(key -> ObjectIdentifier.builder().key(key).build())
                        .toList()))
                .build();

        try (S3Client s3Client = awsClientFactory.getS3Client()) {
            DeleteObjectsResponse response = s3Client.deleteObjects(deleteObjectsRequest);
            log.info("삭제된 객체: {}", response.deleted());
        } catch (S3Exception e) {
            log.error("S3 객체 삭제 중 오류 발생: {}", e.awsErrorDetails().errorMessage());
            throw new ClientRuntimeException(e.getMessage());
        }
    }

    public String getPreSignedUrl(String s3ObjectKey) {
        try (S3Presigner s3Presigner = awsClientFactory.getS3Presigner()) {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .getObjectRequest(builder -> builder
                            .bucket(awsProperty.getBucketName())
                            .key(s3ObjectKey))
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        }
    }
}
