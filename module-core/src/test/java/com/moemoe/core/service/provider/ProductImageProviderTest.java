package com.moemoe.core.service.provider;

import com.moemoe.client.aws.AwsS3Client;
import com.moemoe.client.aws.dto.S3ObjectStream;
import com.moemoe.core.response.GetProductImageServiceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

@ExtendWith(MockitoExtension.class)
class ProductImageProviderTest {
    @Nested
    @DisplayName("provide 메서드 테스트")
    class ProvideTests {
        @Mock
        AwsS3Client awsS3Client;
        @InjectMocks
        ProductImageProvider provider;

        private S3ObjectStream stream(String content, String contentType, long len) {
            return new S3ObjectStream(
                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                    contentType,
                    len
            );
        }

        @Test
        @DisplayName("Should return response when valid object stream is provided")
        void shouldReturnResponseWhenValidObjectStreamIsProvided() throws Exception {
            // given
            String imageKey = "images/p-1/1.jpg";
            byte[] payload = "ok".getBytes(StandardCharsets.UTF_8);
            given(awsS3Client.getObjectStream(imageKey))
                    .willReturn(stream("ok", IMAGE_JPEG_VALUE, payload.length));

            // when
            GetProductImageServiceResponse res = provider.provide(imageKey);

            // then (핵심 속성 한방 검증)
            assertThat(res).extracting(
                    GetProductImageServiceResponse::fallback,
                    GetProductImageServiceResponse::isInMemory,
                    GetProductImageServiceResponse::contentType,
                    GetProductImageServiceResponse::contentLength
            ).containsExactly(false, false, IMAGE_JPEG_VALUE, (long) payload.length);

            // 바디까지 확인(스트림)
            try (res) {
                assertThat(res.inputStream().readAllBytes()).containsExactly(payload);
            }
            then(awsS3Client).should().getObjectStream(imageKey);
        }

        @Test
        @DisplayName("Should return fallback when imageKey is null or blank")
        void shouldReturnFallbackWhenImageKeyIsNullOrBlank() throws Exception {
            // given
            given(awsS3Client.getObjectStream("")).willReturn(stream("fb", IMAGE_JPEG_VALUE, 2L));

            // when
            GetProductImageServiceResponse res1 = provider.provide(null);
            GetProductImageServiceResponse res2 = provider.provide("  ");

            // then (두 케이스를 한 번에 검증)
            assertThat(List.of(res1, res2))
                    .extracting(
                            GetProductImageServiceResponse::fallback,
                            GetProductImageServiceResponse::isInMemory,
                            GetProductImageServiceResponse::contentType,
                            GetProductImageServiceResponse::contentLength
                    )
                    .containsExactly(
                            tuple(true, false, IMAGE_JPEG_VALUE, 2L),
                            tuple(true, false, IMAGE_JPEG_VALUE, 2L)
                    );

            then(awsS3Client).should(times(2)).getObjectStream("");
        }

        @Test
        @DisplayName("Should return fallback when primary throws and fallback succeeds")
        void shouldReturnFallbackWhenPrimaryThrowsAndFallbackSucceeds() throws Exception {
            // given
            String imageKey = "images/p-1/missing.jpg";
            willThrow(new RuntimeException("boom")).given(awsS3Client).getObjectStream(imageKey);
            given(awsS3Client.getObjectStream("")).willReturn(stream("fb", IMAGE_JPEG_VALUE, 2L));

            // when
            GetProductImageServiceResponse res = provider.provide(imageKey);

            // then
            assertThat(res).extracting(
                    GetProductImageServiceResponse::fallback,
                    GetProductImageServiceResponse::isInMemory,
                    GetProductImageServiceResponse::contentType,
                    GetProductImageServiceResponse::contentLength
            ).containsExactly(true, false, IMAGE_JPEG_VALUE, 2L);

            then(awsS3Client).should().getObjectStream(imageKey);
            then(awsS3Client).should().getObjectStream("");
        }

        @Test
        @DisplayName("Should return tinyGif when both primary and fallback throw")
        void shouldReturnTinyGifWhenBothPrimaryAndFallbackThrow() {
            // given
            String imageKey = "images/p-1/missing.jpg";
            willThrow(new RuntimeException("boom-1")).given(awsS3Client).getObjectStream(imageKey);
            willThrow(new RuntimeException("boom-2")).given(awsS3Client).getObjectStream("");

            // when
            GetProductImageServiceResponse res = provider.provide(imageKey);

            // then (핵심 속성 한방 검증)
            assertThat(res).extracting(
                    GetProductImageServiceResponse::fallback,
                    GetProductImageServiceResponse::isInMemory,
                    GetProductImageServiceResponse::contentType
            ).containsExactly(true, true, "image/gif");

            // 길이/스트림도 간단 검증
            assertThat(res.contentLength()).isGreaterThan(0L);
            assertThat(res.inputStream()).isNull();

            then(awsS3Client).should().getObjectStream(imageKey);
            then(awsS3Client).should().getObjectStream("");
        }
    }
}
