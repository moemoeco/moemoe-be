package com.moemoe.client.aws.dto;

public record PresignedFile(
        String fileName,
        String uploadUrl,
        String fileUrl
) {
}
