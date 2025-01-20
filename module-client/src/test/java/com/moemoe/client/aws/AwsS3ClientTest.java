package com.moemoe.client.aws;

import com.moemoe.client.aws.dto.AwsProperty;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@Slf4j
@Testcontainers
@SpringBootTest(classes = AwsS3Client.class)
class AwsS3ClientTest {
    @Autowired
    private AwsS3Client awsS3Client;
    @MockBean
    private AwsProperty awsProperty;
    private static final String BUCKET_NAME = "test";
    private static S3Client s3Client;
    private static S3Presigner s3Presigner;

    @Container
    static LocalStackContainer container = new LocalStackContainer()
            .withServices(LocalStackContainer.Service.S3);

    @BeforeAll
    static void init() {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(
                container.getAccessKey(),
                container.getSecretKey()
        );
        Region region = Region.of(container.getRegion());
        // create s3 client
        s3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .endpointOverride(container.getEndpoint())
                .region(region)
                .build();
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket(BUCKET_NAME)
                .build();
        s3Client.createBucket(createBucketRequest);

        // create s3 presigner
        s3Presigner = S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .endpointOverride(container.getEndpoint())
                .region(region)
                .build();
    }

    @BeforeEach
    void setUp() {
        given(awsProperty.getAccessKey())
                .willReturn(container.getAccessKey());
        given(awsProperty.getSecretKey())
                .willReturn(container.getSecretKey());
        given(awsProperty.getRegion())
                .willReturn(Region.of(container.getRegion()));
        given(awsProperty.getBucketName())
                .willReturn(BUCKET_NAME);
    }

    @AfterEach
    void cleanup() {
        String continuationToken = null;

        do {
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(BUCKET_NAME)
                    .continuationToken(continuationToken)
                    .build();

            ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);

            listObjectsResponse.contents().forEach(s3Object -> {
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(s3Object.key())
                        .build();
                s3Client.deleteObject(deleteObjectRequest);
            });

            continuationToken = listObjectsResponse.nextContinuationToken();

        } while (continuationToken != null);
    }


    @AfterAll
    static void destroy() {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                .bucket(BUCKET_NAME)
                .build();
        s3Client.deleteBucket(deleteBucketRequest);

    }

    @Test
    void upload() throws IOException {
        // given
        String fileName = "image.jpg";
        byte[] fileContent = "test content".getBytes();
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "file",
                fileName,
                IMAGE_JPEG_VALUE,
                new ByteArrayInputStream(fileContent)
        );

        String s3ObjectKey = "uploads/" + fileName;

        // when
        String returnedKey = awsS3Client.upload(s3Client, s3ObjectKey, mockMultipartFile);
        assertThat(returnedKey)
                .isEqualTo(s3ObjectKey);

        // then
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3ObjectKey)
                .build();
        HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
        assertThat(headObjectResponse)
                .isNotNull();
        assertThat(headObjectResponse.contentLength())
                .isEqualTo(fileContent.length);
    }

    @Test
    void getPreSignedUrl() throws IOException {
        // given
        String fileName = "image.jpg";
        byte[] fileContent = "test content".getBytes();
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "file",
                fileName,
                IMAGE_JPEG_VALUE,
                new ByteArrayInputStream(fileContent)
        );

        String s3ObjectKey = "uploads/" + fileName;
        uploadMultipartFile(s3ObjectKey, mockMultipartFile);

        // when
        String preSignedUrl = awsS3Client.getPreSignedUrl(s3Presigner, s3ObjectKey);

        // then
        assertThat(preSignedUrl)
                .isNotEmpty()
                .contains(List.of(s3ObjectKey));

        HttpURLConnection connection = (HttpURLConnection) new URL(preSignedUrl).openConnection();
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();

        assertThat(responseCode)
                .isEqualTo(HttpURLConnection.HTTP_OK);
        String downloadedContent = new String(connection.getInputStream().readAllBytes());
        assertThat(downloadedContent)
                .isEqualTo("test content");
    }

    private void uploadMultipartFile(String s3ObjectKey, MultipartFile multipartFile) {
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
            } else {
                throw new IllegalArgumentException("S3 upload failed: Status code : " + sdkHttpResponse.statusCode());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("An error occurred during file upload: " + e.getMessage(), e);
        }
    }
}