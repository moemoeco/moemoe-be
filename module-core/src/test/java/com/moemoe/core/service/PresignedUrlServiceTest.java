package com.moemoe.core.service;

import com.moemoe.client.aws.AwsS3PresignedClient;
import com.moemoe.client.aws.dto.PresignedFile;
import com.moemoe.core.request.GeneratePresignedUrlServiceRequest;
import com.moemoe.core.response.GeneratePresignedUrlServiceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PresignedUrlServiceTest {
    @InjectMocks
    private PresignedUrlService presignedUrlService;
    @Mock
    private AwsS3PresignedClient awsS3PresignedClient;

    @Test
    @DisplayName("성공 케이스 : 파일 목록에 대해 Presigned URL 생성")
    void generatePresignedUrl() {
        // given
        List<GeneratePresignedUrlServiceRequest.FileRequestDto> fileRequests = List.of(
                new GeneratePresignedUrlServiceRequest.FileRequestDto("image1.jpg", "image/jpeg"),
                new GeneratePresignedUrlServiceRequest.FileRequestDto("image2.png", "image/png")
        );
        GeneratePresignedUrlServiceRequest request = new GeneratePresignedUrlServiceRequest(fileRequests);

        given(awsS3PresignedClient.generatePresignedUrl("image1.jpg", "image/jpeg"))
                .willReturn(new PresignedFile("image1.jpg", "https://upload.url/1", "products/images/uuid1_image1.jpg"));
        given(awsS3PresignedClient.generatePresignedUrl("image2.png", "image/png"))
                .willReturn(new PresignedFile("image2.png", "https://upload.url/2", "products/images/uuid2_image2.png"));

        // when
        GeneratePresignedUrlServiceResponse response = presignedUrlService.generatePresignedUrl(request);

        // then
        assertThat(response)
                .isNotNull();
        assertThat(response.files())
                .hasSize(2)
                .extracting(
                        GeneratePresignedUrlServiceResponse.PresignedFileDto::filename,
                        GeneratePresignedUrlServiceResponse.PresignedFileDto::uploadUrl,
                        GeneratePresignedUrlServiceResponse.PresignedFileDto::fileKey
                )
                .containsExactlyInAnyOrder(
                        tuple("image1.jpg", "https://upload.url/1", "products/images/uuid1_image1.jpg"),
                        tuple("image2.png", "https://upload.url/2", "products/images/uuid2_image2.png")
                );
    }
}