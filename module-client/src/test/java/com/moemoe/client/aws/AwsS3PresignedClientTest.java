package com.moemoe.client.aws;

import com.moemoe.client.aws.property.AwsProperty;
import com.moemoe.client.aws.dto.PresignedFile;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@Slf4j
@Testcontainers
@SpringBootTest(classes = AwsS3PresignedClient.class)
class AwsS3PresignedClientTest {
    @Autowired
    private AwsS3PresignedClient awsS3PresignedClient;
    @MockBean
    private AwsProperty awsProperty;
    @MockBean
    private AwsClientFactory awsClientFactory;
    @Container
    static LocalStackContainer container = new LocalStackContainer(DockerImageName.parse("localstack/localstack").withTag("0.11.2"))
            .withServices(LocalStackContainer.Service.S3);
    private static final String BUCKET_NAME = "test";
    private static AwsBasicCredentials awsBasicCredentials;
    private static Region region;

    @BeforeAll
    static void createTestClient() {
        awsBasicCredentials = AwsBasicCredentials.create(
                container.getAccessKey(),
                container.getSecretKey()
        );
        region = Region.of(container.getRegion());

        // create a test bucket
        try (S3Client testS3Client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .endpointOverride(container.getEndpoint())
                .region(region)
                .build();) {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(BUCKET_NAME)
                    .build();
            testS3Client.createBucket(createBucketRequest);
        }
    }

    @Test
    @DisplayName("성공 케이스 : 정상 생성")
    void generatePresignedUrl() {
        // given
        S3Presigner s3Presigner = S3Presigner.builder()
                .endpointOverride(container.getEndpoint())
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .region(region)
                .build();
        given(awsClientFactory.getS3Presigner())
                .willReturn(s3Presigner);
        given(awsProperty.getBucketName())
                .willReturn(BUCKET_NAME);

        String fileName = "fileName";
        String imagePng = MediaType.IMAGE_PNG_VALUE;

        // when
        PresignedFile presignedFile = awsS3PresignedClient.generatePresignedUrl(fileName, imagePng);

        // then
        assertThat(presignedFile)
                .isNotNull();
        assertThat(presignedFile.fileName())
                .isEqualTo(fileName);
        assertThat(presignedFile.fileKey())
                .startsWith("products/images/")
                .endsWith("_" + fileName);
        assertThat(presignedFile.uploadUrl())
                .contains(awsProperty.getBucketName())
                .contains(presignedFile.fileKey())
                .contains("X-Amz-Signature")
                .contains("X-Amz-Algorithm")
                .contains("X-Amz-Date");
    }
}