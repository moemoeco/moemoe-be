package com.moemoe.core.response;

import com.moemoe.client.aws.dto.PresignedFile;

import java.util.List;

public record GeneratePresignedUrlServiceResponse(
        List<PresignedFileDto> files
) {
    public record PresignedFileDto(
            String filename,
            String uploadUrl,
            String fileKey
    ) {
        public static PresignedFileDto from(PresignedFile presignedFile) {
            return new PresignedFileDto(presignedFile.fileName(), presignedFile.uploadUrl(), presignedFile.fileKey());
        }
    }
}
