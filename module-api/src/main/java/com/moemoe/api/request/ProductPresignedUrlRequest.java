package com.moemoe.api.request;

import com.moemoe.api.validation.annotation.ImageContentType;
import com.moemoe.core.request.GeneratePresignedUrlServiceRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductPresignedUrlRequest(
        @Size(min = 1, max = 10, message = "Images must include at least 1 item and up to 10 items.")
        List<ProductRequest> images
) {
    public GeneratePresignedUrlServiceRequest toServiceRequest() {
        List<GeneratePresignedUrlServiceRequest.FileRequestDto> fileRequestDtos = images.stream()
                .map(ProductRequest::toDto)
                .toList();
        return new GeneratePresignedUrlServiceRequest(fileRequestDtos);
    }

    public record ProductRequest(
            @NotBlank String fileName,
            @NotBlank @ImageContentType String contentType
    ) {
        private GeneratePresignedUrlServiceRequest.FileRequestDto toDto() {
            return new GeneratePresignedUrlServiceRequest.FileRequestDto(fileName, contentType);
        }
    }
}
