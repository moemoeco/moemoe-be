package com.moemoe.core.response;

import java.io.Closeable;
import java.io.InputStream;

public record GetProductImageServiceResponse(
        InputStream inputStream,
        byte[] inline,
        String contentType,
        Long contentLength,
        boolean fallback
) implements AutoCloseable {

    public static GetProductImageServiceResponse ofStream(InputStream inputStream, String contentType, Long contentLength, boolean fallback) {
        String safeCt = (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;
        if (contentLength == null || contentLength <= 0L) {
            return tinyGif();
        }
        return new GetProductImageServiceResponse(inputStream, null, safeCt, contentLength, fallback);
    }

    public static GetProductImageServiceResponse tinyGif() {
        // 1x1 transparent GIF
        byte[] gif = new byte[]{
                0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00, (byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x21, (byte) 0xf9, 0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x2c, 0x00, 0x00, 0x00, 0x00,
                0x01, 0x00, 0x01, 0x00, 0x00, 0x02, 0x02, 0x44, 0x01, 0x00, 0x3b
        };
        return new GetProductImageServiceResponse(null, gif, "image/gif", (long) gif.length, true);
    }

    public boolean isInMemory() {
        return inline != null;
    }

    @Override
    public void close() {
        if (inputStream instanceof Closeable c) {
            try {
                c.close();
            } catch (Exception ignore) {
            }
        }
    }
}
