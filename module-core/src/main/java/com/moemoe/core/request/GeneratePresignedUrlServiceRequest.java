package com.moemoe.core.request;

import java.util.List;

public record GeneratePresignedUrlServiceRequest(
        List<FileRequestDto> files
) {
    public record FileRequestDto(
            String fileName,
            String contentType
    ) {
    }
}
