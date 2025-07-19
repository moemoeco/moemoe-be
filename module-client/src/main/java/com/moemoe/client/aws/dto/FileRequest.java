package com.moemoe.client.aws.dto;

public record FileRequest(
        String fileName,
        String contentType
) {
}
