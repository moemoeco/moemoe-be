package com.moemoe.client.aws.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.regions.Region;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "cloud.aws.accessKey=testAccessKey",
        "cloud.aws.secretKey=testSecretKey",
        "cloud.aws.bucketName=testBucketName",
        "cloud.aws.region=ap-northeast-2"
})
@EnableConfigurationProperties(AwsProperty.class)
class AwsPropertyTest {
    @Autowired
    private AwsProperty awsProperty;

    @Test
    void propertyBinding() {
        // Then
        assertThat(awsProperty.getAccessKey())
                .isEqualTo("testAccessKey");
        assertThat(awsProperty.getSecretKey())
                .isEqualTo("testSecretKey");
        assertThat(awsProperty.getBucketName())
                .isEqualTo("testBucketName");
        assertThat(awsProperty.getRegion())
                .isEqualTo(Region.AP_NORTHEAST_2);
    }

}