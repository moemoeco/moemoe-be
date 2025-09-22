package com.moemoe.core.service.resolver;

import com.moemoe.core.property.CdnProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductImageKeyResolverTest {
    @Nested
    @DisplayName("resolve 메서드 테스트")
    class ResolveTests {
        @Test
        @DisplayName("Should build URL when inputs are normal")
        void shouldBuildUrlWhenInputsAreNormal() {
            // given
            CdnProperty prop = new CdnProperty("http://localhost:9000");
            ProductImageKeyResolver resolver = new ProductImageKeyResolver(prop);

            // when
            String url = resolver.resolve("images/test/test.jpg");

            // then
            assertThat(url)
                    .isEqualTo("http://localhost:9000/cdn/product-images/images/test/test.jpg");
        }

        @Test
        @DisplayName("Should normalize when proxy has trailing slash")
        void shouldNormalizeWhenProxyHasTrailingSlash() {
            // given
            CdnProperty prop = new CdnProperty("http://localhost:9000/");
            ProductImageKeyResolver resolver = new ProductImageKeyResolver(prop);

            // when
            String url = resolver.resolve("images/test/test.jpg");

            // then
            assertThat(url)
                    .isEqualTo("http://localhost:9000/cdn/product-images/images/test/test.jpg");
        }

        @Test
        @DisplayName("Should ignore leading slashes in key")
        void shouldIgnoreLeadingSlashesInKey() {
            // given
            CdnProperty prop = new CdnProperty("http://localhost:9000");
            ProductImageKeyResolver resolver = new ProductImageKeyResolver(prop);

            // when
            String url = resolver.resolve("/images/test/test.jpg");

            // then
            assertThat(url)
                    .isEqualTo("http://localhost:9000/cdn/product-images/images/test/test.jpg");
        }

        @Test
        @DisplayName("Should encode special characters per segment")
        void shouldEncodeSpecialCharactersPerSegment() {
            // given
            CdnProperty prop = new CdnProperty("http://localhost:9000");
            ProductImageKeyResolver resolver = new ProductImageKeyResolver(prop);

            // when
            String url = resolver.resolve("images/a b/c#d/ef.jpg");

            // then
            assertThat(url)
                    .isEqualTo("http://localhost:9000/cdn/product-images/images/a%20b/c%23d/ef.jpg");
        }
    }
}