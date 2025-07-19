package com.moemoe.client.aws.dto;

public record PresignedFile(
        String fileName,
        String uploadUrl,
        String fileKey
) {
    public static PresignedFile of(String fileName, String uploadUrl, String fileKey) {
        return new PresignedFile(fileName, uploadUrl, fileKey);
    }
}
