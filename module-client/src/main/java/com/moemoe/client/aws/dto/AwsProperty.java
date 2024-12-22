package com.moemoe.client.aws.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cloud.aws")
public class AwsProperty {
    private String accessKey;
    private String secretKey;
    private Region region;
}
