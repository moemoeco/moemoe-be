package com.moemoe.core.service;

import com.moemoe.client.aws.AwsS3PresignedClient;
import com.moemoe.core.request.GeneratePresignedUrlServiceRequest;
import com.moemoe.core.response.GeneratePresignedUrlServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PresignedUrlService {
    private final AwsS3PresignedClient awsS3PresignedClient;

    public GeneratePresignedUrlServiceResponse generatePresignedUrl(GeneratePresignedUrlServiceRequest request) {
        List<GeneratePresignedUrlServiceResponse.PresignedFileDto> presignedFileDtos = request.files()
                .stream()
                .map(fileRequest -> awsS3PresignedClient.generatePresignedUrl(fileRequest.fileName(), fileRequest.contentType()))
                .map(GeneratePresignedUrlServiceResponse.PresignedFileDto::from)
                .toList();
        return new GeneratePresignedUrlServiceResponse(presignedFileDtos);
    }
}
