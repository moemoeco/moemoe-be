package com.moemoe.api.controller;

import com.moemoe.api.AbstractControllerTest;
import com.moemoe.core.response.GetProductImageServiceResponse;
import com.moemoe.core.service.provider.ProductImageProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CdnProxyController.class)
class CdnProxyControllerTest extends AbstractControllerTest {
    @MockBean
    ProductImageProvider productImageProvider;

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("GET /cdn/product-images/{*imageKey}")
    class GetProductImages {

        @Test
        @DisplayName("Should stream original when fallback=false and inMemory=false")
        void shouldStreamOriginalWhenNotFallbackAndNotInMemory() {
            // given
            String key = "images/p-1/1.jpg";
            byte[] payload = bytes("ORIGINAL");
            GetProductImageServiceResponse serviceResponse = GetProductImageServiceResponse.ofStream(
                    new ByteArrayInputStream(payload),
                    MediaType.IMAGE_JPEG_VALUE,
                    (long) payload.length,
                    false
            );
            given(productImageProvider.provide(key))
                    .willReturn(serviceResponse);

            // when
            MvcResult mvc = invoke(
                    MockMvcRequestBuilders.get("/cdn/product-images/" + key),
                    status().isOk(),
                    true
            );

            MockHttpServletResponse response = mvc.getResponse();
            String cache = response.getHeader("Cache-Control");
            assertThat(cache)
                    .contains("max-age=31536000")
                    .contains("public")
                    .contains("immutable");
            assertThat(response)
                    .extracting(MockHttpServletResponse::getContentType,
                            res -> res.getHeader("Content-Length"),
                            MockHttpServletResponse::getContentAsByteArray)
                    .containsExactly(
                            MediaType.IMAGE_JPEG_VALUE,
                            String.valueOf(payload.length),
                            payload
                    );
            then(productImageProvider).should()
                    .provide(key);
        }


        @Test
        @DisplayName("Should stream fallback when fallback=true and inMemory=false")
        void shouldStreamFallbackWhenFallbackTrueAndNotInMemory() {
            // given
            String key = "images/p-1/missing.jpg";
            byte[] payload = bytes("FALLBACK_STREAM");
            GetProductImageServiceResponse serviceResponse = GetProductImageServiceResponse.ofStream(
                    new ByteArrayInputStream(payload),
                    MediaType.IMAGE_JPEG_VALUE,
                    (long) payload.length,
                    true // fallback
            );
            given(productImageProvider.provide(key)).willReturn(serviceResponse);

            // when
            MvcResult mvc = invoke(
                    MockMvcRequestBuilders.get("/cdn/product-images/" + key),
                    status().isOk(),
                    true
            );

            // then
            MockHttpServletResponse response = mvc.getResponse();
            assertThat(response.getHeader("Cache-Control"))
                    .contains("max-age=60")
                    .contains("public");

            assertThat(response)
                    .extracting(
                            MockHttpServletResponse::getContentType,
                            res -> res.getHeader("Content-Length"),
                            MockHttpServletResponse::getContentAsByteArray
                    )
                    .containsExactly(
                            MediaType.IMAGE_JPEG_VALUE,
                            String.valueOf(payload.length),
                            payload
                    );

            then(productImageProvider).should().provide(key);
        }

        @Test
        @DisplayName("Should write inline bytes when fallback=true and inMemory=true")
        void shouldWriteInlineWhenFallbackTrueAndInMemoryTrue() {
            // given
            String key = "images/p-1/fb-inline.jpg";
            byte[] inline = bytes("INLINE_BYTES");
            GetProductImageServiceResponse serviceResponse = new GetProductImageServiceResponse(
                    null,                           // inputStream
                    inline,                         // inline bytes
                    MediaType.IMAGE_PNG_VALUE,      // contentType
                    (long) inline.length,           // contentLength
                    true                            // fallback
            );
            given(productImageProvider.provide(key)).willReturn(serviceResponse);

            // when
            MvcResult mvc = invoke(
                    MockMvcRequestBuilders.get("/cdn/product-images/" + key),
                    status().isOk(),
                    true
            );

            // then
            MockHttpServletResponse response = mvc.getResponse();
            assertThat(response.getHeader("Cache-Control"))
                    .contains("max-age=60")
                    .contains("public");

            assertThat(response)
                    .extracting(
                            MockHttpServletResponse::getContentType,
                            res -> res.getHeader("Content-Length"),
                            MockHttpServletResponse::getContentAsByteArray
                    )
                    .containsExactly(
                            MediaType.IMAGE_PNG_VALUE,
                            String.valueOf(inline.length),
                            inline
                    );

            then(productImageProvider).should().provide(key);
        }

        @Test
        @DisplayName("Should return 500 when provider throws RuntimeException")
        void shouldReturn500WhenProviderThrows() {
            // given
            String key = "images/p-1/error.jpg";
            willThrow(new IllegalArgumentException("boom"))
                    .given(productImageProvider).provide(key);

            // when
            MvcResult mvc = invoke(
                    MockMvcRequestBuilders.get("/cdn/product-images/" + key),
                    status().isInternalServerError(),
                    true
            );

            // then
            assertThat(mvc.getResponse().getStatus()).isEqualTo(500);
            then(productImageProvider).should().provide(key);
        }
    }
}
