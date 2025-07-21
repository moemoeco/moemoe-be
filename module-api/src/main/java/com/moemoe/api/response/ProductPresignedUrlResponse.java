package com.moemoe.api.response;

import com.moemoe.core.response.GeneratePresignedUrlServiceResponse;

import java.util.List;

public record ProductPresignedUrlResponse(
        List<ProductResponse> presignedUrls
) {
    public static ProductPresignedUrlResponse fromServiceResponse(GeneratePresignedUrlServiceResponse serviceResponse) {
        List<ProductResponse> responses = serviceResponse.files()
                .stream()
                .map(file -> new ProductResponse(file.filename(), file.uploadUrl(), file.fileKey()))
                .toList();
        return new ProductPresignedUrlResponse(responses);
    }

    public record ProductResponse(
            String filename,
            String uploadUrl,
            String fileKey
    ) {
    }
}

