package com.moemoe.client.aws.dto;

import java.io.InputStream;

public record S3ObjectStream(InputStream stream,
                             String contentType,
                             long contentLength) implements AutoCloseable {
    @Override
    public void close() {
        try {
            stream.close();
        } catch (Exception ignore) {
        }
    }
}
