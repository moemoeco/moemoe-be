package com.moemoe.client.aws.dto;

import com.moemoe.client.config.PropertyConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.regions.Region;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AwsPropertyTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(PropertyConfiguration.class);

    @Test
    @DisplayName("성공 케이스 : aws property 바인딩 성공")
    void testWithS3Properties() {
        contextRunner
                .withPropertyValues(
                        "aws.region=ap-northeast-2",
                        "aws.credentials.accessKey=key",
                        "aws.credentials.secretKey=secret",
                        "aws.s3.bucketName=bucket"
                )
                .run(context -> {
                    AwsProperty aws = context.getBean(AwsProperty.class);
                    assertEquals("key", aws.getAccessKey());
                    assertEquals("secret", aws.getSecretKey());
                    assertEquals("bucket", aws.getBucketName());
                    assertEquals(Region.AP_NORTHEAST_2, aws.getRegion());
                });
    }

    @Test
    @DisplayName("실패 케이스 : aws s3 property 바인딩 실패")
    void testWithoutS3Properties() {
        contextRunner
                .withPropertyValues("aws.region=ap-northeast-2",
                        "aws.credentials.accessKey=key",
                        "aws.credentials.secretKey=secret")
                .run(context -> {
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure)
                            .isInstanceOf(ConfigurationPropertiesBindException.class);
                });
    }

    @Test
    @DisplayName("실패 케이스 : aws credentials property 바인딩 실패")
    void testWithoutCredentialsProperties() {
        contextRunner
                .withPropertyValues("aws.region=ap-northeast-2",
                        "aws.s3.bucketName=bucket")
                .run(context -> {
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure)
                            .isInstanceOf(ConfigurationPropertiesBindException.class);
                });
    }

    @Test
    @DisplayName("실패 케이스 : aws property 바인딩 실패")
    void testWithoutAwsProperties() {
        contextRunner
                .run(context -> {
                    Throwable failure = context.getStartupFailure();
                    assertThat(failure)
                            .isInstanceOf(ConfigurationPropertiesBindException.class);
                });
    }
}