package com.moemoe.client.aws;

import com.moemoe.client.aws.dto.AwsProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class AwsS3Client {
    private final AwsProperty awsProperty;

    /**
     * 이미지 파일을 S3 버킷에 업로드합니다.
     *
     * <p>이 메서드는 제공된 {@link S3Client}를 사용하여 주어진 {@link MultipartFile}을 지정된 S3 버킷에 업로드합니다.
     * S3 객체 키는 버킷 내에서 파일을 식별하는 데 사용됩니다. 파일의 입력 스트림은 try-with-resources 블록을 통해 안전하게 처리됩니다.</p>
     *
     * @param s3ObjectKey   S3 버킷 내 업로드된 객체를 식별하기 위한 고유 키
     * @param multipartFile 업로드할 파일
     * @return 업로드가 성공하면 S3 객체 키를 반환
     * @throws IllegalArgumentException 업로드 실패 또는 I/O 오류 발생 시 예외를 발생
     */
    public String upload(S3Client s3Client, String s3ObjectKey, MultipartFile multipartFile) {
        try (InputStream inputStream = multipartFile.getInputStream()) {
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

    public S3Client getS3Client() {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(
                awsProperty.getAccessKey(),
                awsProperty.getSecretKey()
        );
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .region(awsProperty.getRegion())
                .build();
    }
}
