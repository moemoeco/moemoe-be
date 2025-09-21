package com.moemoe.client.aws;

import com.moemoe.client.aws.property.AwsProperty;
import com.moemoe.client.exception.ClientRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
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
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
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
    @MockBean
    private AwsClientFactory awsClientFactory;
    private static final String BUCKET_NAME = "test";
    private static S3Client testS3Client;

    @Container
    static LocalStackContainer container = new LocalStackContainer()
            .withServices(LocalStackContainer.Service.S3);

    @BeforeAll
    static void createTestClient() {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(
                container.getAccessKey(),
                container.getSecretKey()
        );
        Region region = Region.of(container.getRegion());
        // create s3 client
        testS3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .endpointOverride(container.getEndpoint())
                .region(region)
                .build();

        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket(BUCKET_NAME)
                .build();
        testS3Client.createBucket(createBucketRequest);
    }

    @BeforeEach
    void setUp() {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(
                container.getAccessKey(),
                container.getSecretKey()
        );
        Region region = Region.of(container.getRegion());

        given(awsProperty.getBucketName())
                .willReturn(BUCKET_NAME);
        given(awsClientFactory.getS3Client())
                .willReturn(S3Client.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                        .endpointOverride(container.getEndpoint())
                        .region(region)
                        .build());
        given(awsClientFactory.getS3Presigner())
                .willReturn(S3Presigner.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                        .endpointOverride(container.getEndpoint())
                        .region(region)
                        .build());
    }

    @AfterEach
    void cleanup() {
        String continuationToken = null;
        // create s3 client

        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(
                container.getAccessKey(),
                container.getSecretKey()
        );
        Region region = Region.of(container.getRegion());

        try (S3Client cleanUpS3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .endpointOverride(container.getEndpoint())
                .region(region)
                .build()) {

            do {
                ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                        .bucket(BUCKET_NAME)
                        .continuationToken(continuationToken)
                        .build();

                ListObjectsV2Response listObjectsResponse = cleanUpS3Client.listObjectsV2(listObjectsRequest);

                listObjectsResponse.contents().forEach(s3Object -> {
                    DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                            .bucket(BUCKET_NAME)
                            .key(s3Object.key())
                            .build();
                    cleanUpS3Client.deleteObject(deleteObjectRequest);
                });

                continuationToken = listObjectsResponse.nextContinuationToken();

            } while (continuationToken != null);
        }
    }


    @AfterAll
    static void destroy() {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                .bucket(BUCKET_NAME)
                .build();
        testS3Client.deleteBucket(deleteBucketRequest);
    }

    @Nested
    @DisplayName("getObjectStream 메서드 테스트 (LocalStack + spy)")
    class GetObjectStreamTests {

        private S3Client spyS3;

        @BeforeEach
        void useSpyClient() {
            // 실제 LocalStack S3Client 생성
            AwsBasicCredentials creds = AwsBasicCredentials.create(container.getAccessKey(), container.getSecretKey());
            Region region = Region.of(container.getRegion());
            S3Client real = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(creds))
                    .endpointOverride(container.getEndpoint())
                    .region(region)
                    .build();

            // spy 래핑 (기본은 실제 호출)
            spyS3 = Mockito.spy(real);

            // AwsClientFactory가 spy를 반환하도록 오버라이드
            given(awsClientFactory.getS3Client())
                    .willReturn(spyS3);
            given(awsProperty.getBucketName())
                    .willReturn(BUCKET_NAME);
        }

        private void put(String key, byte[] bytes, String contentType) {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key)
                    .contentType(contentType)
                    .build();
            testS3Client.putObject(put, RequestBody.fromBytes(bytes));
        }

        private ResponseInputStream<GetObjectResponse> createResponseInputStream(byte[] data, String contentType, Long contentLength) {
            GetObjectResponse meta = GetObjectResponse.builder()
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();
            return new ResponseInputStream<>(meta,
                    AbortableInputStream.create(new ByteArrayInputStream(data)));
        }

        @Test
        @DisplayName("Should return stream when valid object exists in S3")
        void shouldReturnStreamWhenValidObjectExists() throws Exception {
            // given
            String key = "images/p-1/thumb/main.jpg";
            byte[] payload = new byte[]{1, 2, 3};
            put(key, payload, IMAGE_JPEG_VALUE); // 실제 LocalStack 업로드

            // when
            var result = awsS3Client.getObjectStream(key);

            // then
            assertThat(result.contentType()).isEqualTo(IMAGE_JPEG_VALUE);
            assertThat(result.contentLength()).isEqualTo(payload.length);
            try (result) {
                assertThat(result.stream().readAllBytes()).containsExactly(payload);
            }
        }

        @Test
        @DisplayName("Should throw ClientRuntimeException when S3 getObject throws S3Exception")
        void shouldThrowWhenS3Throws() {
            // given: 특정 키에 대해서만 예외를 강제로 던지도록 spy 스텁
            String boomKey = "images/boom.jpg";
            BDDMockito.doAnswer(inv -> {
                GetObjectRequest req = inv.getArgument(0);
                if (boomKey.equals(req.key())) {
                    throw S3Exception.builder().awsErrorDetails(AwsErrorDetails.builder().errorMessage("NoSuchKey").build()).statusCode(404).message("NoSuchKey").build();
                }
                return inv.callRealMethod(); // 그 외는 실제 LocalStack 호출
            }).when(spyS3).getObject(ArgumentMatchers.any(GetObjectRequest.class));

            // when/then
            assertThatThrownBy(() -> awsS3Client.getObjectStream(boomKey))
                    .isInstanceOf(ClientRuntimeException.class)
                    .hasMessageContaining("NoSuchKey");
        }

        @Test
        @DisplayName("Should throw ClientRuntimeException when key is null or blank")
        void shouldThrowWhenKeyNullOrBlank() {
            // when/then
            assertThatThrownBy(() -> awsS3Client.getObjectStream(null))
                    .isInstanceOf(ClientRuntimeException.class);
            assertThatThrownBy(() -> awsS3Client.getObjectStream(" "))
                    .isInstanceOf(ClientRuntimeException.class);
            // 유효성 검사가 선행되므로 S3 호출이 없어야 함
            Mockito.verify(spyS3, Mockito.never())
                    .getObject(ArgumentMatchers.any(GetObjectRequest.class));
        }

        @Test
        @DisplayName("Should throw ClientRuntimeException when contentLength is null")
        void shouldThrowWhenContentLengthNull() {
            // given: 실제 S3에선 null 길이가 없으므로 spy로 메타만 조작
            String key = "images/null-length.jpg";
            Mockito.doReturn(createResponseInputStream(new byte[]{1, 2, 3}, IMAGE_JPEG_VALUE, null))
                    .when(spyS3).getObject(ArgumentMatchers.any(GetObjectRequest.class));

            // when/then
            assertThatThrownBy(() -> awsS3Client.getObjectStream(key))
                    .isInstanceOf(ClientRuntimeException.class)
                    .hasMessageContaining("Invalid content length");
        }

        @Test
        @DisplayName("Should default contentType to application/octet-stream when S3 returns null")
        void shouldDefaultContentTypeWhenNull() throws Exception {
            // given: contentType만 null로 리턴하도록 spy
            String key = "images/null-ct.jpg";
            Mockito.doReturn(createResponseInputStream(new byte[]{9, 9, 9}, null, 3L))
                    .when(spyS3).getObject(ArgumentMatchers.any(GetObjectRequest.class));

            // when
            var result = awsS3Client.getObjectStream(key);

            // then
            assertThat(result.contentType()).isEqualTo("application/octet-stream");
            try (result) {
                assertThat(result.stream().readAllBytes()).containsExactly(9, 9, 9);
            }
        }
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
        String returnedKey = awsS3Client.upload(s3ObjectKey, mockMultipartFile);
        assertThat(returnedKey)
                .isEqualTo(s3ObjectKey);

        // then
        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3ObjectKey)
                .build();
        HeadObjectResponse headObjectResponse = testS3Client.headObject(headObjectRequest);
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
        String preSignedUrl = awsS3Client.getPreSignedUrl(s3ObjectKey);

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

            PutObjectResponse putObjectResponse = testS3Client.putObject(
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

    @Test
    void delete() throws IOException {
        // given
        String fileName = "image.jpg";
        byte[] fileContent = "test content".getBytes();
        MockMultipartFile mockMultipartFile1 = new MockMultipartFile(
                "file",
                fileName + 1,
                IMAGE_JPEG_VALUE,
                new ByteArrayInputStream(fileContent)
        );
        MockMultipartFile mockMultipartFile2 = new MockMultipartFile(
                "file",
                fileName + 2,
                IMAGE_JPEG_VALUE,
                new ByteArrayInputStream(fileContent)
        );

        String s3ObjectKey1 = "uploads/" + fileName + 1;
        uploadMultipartFile(s3ObjectKey1, mockMultipartFile1);
        String s3ObjectKey2 = "uploads/" + fileName + 2;
        uploadMultipartFile(s3ObjectKey2, mockMultipartFile2);

        boolean existsBeforeDelete1 = isObjectExist(s3ObjectKey1);
        assertThat(existsBeforeDelete1)
                .isTrue();
        boolean existsBeforeDelete2 = isObjectExist(s3ObjectKey2);
        assertThat(existsBeforeDelete2)
                .isTrue();

        // when
        awsS3Client.delete(List.of(s3ObjectKey1, s3ObjectKey2));

        // then
        boolean existsAfterDelete1 = isObjectExist(s3ObjectKey1);
        assertThat(existsAfterDelete1)
                .isFalse();
        boolean existsAfterDelete2 = isObjectExist(s3ObjectKey2);
        assertThat(existsAfterDelete2)
                .isFalse();
    }

    /**
     * S3 버킷에 특정 객체가 존재하는지 확인하는 메서드
     */
    private boolean isObjectExist(String objectKey) {
        try {
            testS3Client.headObject(HeadObjectRequest.builder()
                    .bucket(awsProperty.getBucketName())
                    .key(objectKey)
                    .build());
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

}