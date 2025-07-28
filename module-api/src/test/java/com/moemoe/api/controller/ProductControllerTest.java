package com.moemoe.api.controller;

import com.moemoe.api.AbstractControllerTest;
import com.moemoe.api.config.handler.ErrorResponseBody;
import com.moemoe.api.request.ProductPresignedUrlRequest;
import com.moemoe.api.request.RegisterProductRequest;
import com.moemoe.api.response.ProductPresignedUrlResponse;
import com.moemoe.core.request.GeneratePresignedUrlServiceRequest;
import com.moemoe.core.response.GeneratePresignedUrlServiceResponse;
import com.moemoe.core.response.GetProductsResponse;
import com.moemoe.core.response.IdResponse;
import com.moemoe.core.service.PresignedUrlService;
import com.moemoe.core.service.ProductService;
import com.moemoe.mongo.constant.ProductCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest extends AbstractControllerTest {
    @MockBean
    private ProductService productService;
    @MockBean
    private PresignedUrlService presignedUrlService;

    @Nested
    @DisplayName("Presigned Url 생성 API")
    class GeneratePresignedUrl {
        private final String url = "/products/presigned-urls";

        @Test
        @DisplayName("성공 케이스 : 2개의 presigned url 반환")
        void generatePresignedUrl_success() {
            // given
            ProductPresignedUrlRequest request = new ProductPresignedUrlRequest(List.of(
                    new ProductPresignedUrlRequest.ProductRequest("a.jpg", MediaType.IMAGE_JPEG_VALUE),
                    new ProductPresignedUrlRequest.ProductRequest("b.jpg", MediaType.IMAGE_JPEG_VALUE)
            ));

            GeneratePresignedUrlServiceResponse mockServiceResponse = new GeneratePresignedUrlServiceResponse(List.of(
                    new GeneratePresignedUrlServiceResponse.PresignedFileDto("a.jpg", "https://upload.url/1", "products/images/uuid1_a.jpg"),
                    new GeneratePresignedUrlServiceResponse.PresignedFileDto("b.png", "https://upload.url/2", "products/images/uuid2_b.png")
            ));
            given(presignedUrlService.generatePresignedUrl(any(GeneratePresignedUrlServiceRequest.class)))
                    .willReturn(mockServiceResponse);

            ProductPresignedUrlResponse response = ProductPresignedUrlResponse.fromServiceResponse(mockServiceResponse);

            invoke(
                    post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(convertObjectToJson(request)),
                    status().isOk(),
                    convertObjectToJson(response),
                    true
            );
        }

        @Test
        @DisplayName("실패 케이스 : 요청이 1개 미만")
        void generatePresignedUrl_empty() {
            ProductPresignedUrlRequest request = new ProductPresignedUrlRequest(List.of());

            MvcResult result = invoke(
                    post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(convertObjectToJson(request)),
                    status().isBadRequest(),
                    true
            );

            ErrorResponseBody error = convertResponseToClass(result, ErrorResponseBody.class);
            assertThat(error.getType())
                    .isEqualTo(HandlerMethodValidationException.class.getSimpleName());
        }

        @Test
        @DisplayName("실패 케이스 : 요청이 11개 초과")
        void generatePresignedUrl_exceedLimit() {
            List<ProductPresignedUrlRequest.ProductRequest> oversizedList = new java.util.ArrayList<>();
            for (int i = 0; i < 11; i++) {
                oversizedList.add(new ProductPresignedUrlRequest.ProductRequest(i + ".jpg", MediaType.IMAGE_JPEG_VALUE));
            }
            ProductPresignedUrlRequest request = new ProductPresignedUrlRequest(oversizedList);

            MvcResult result = invoke(
                    post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(convertObjectToJson(request)),
                    status().isBadRequest(),
                    true
            );

            ErrorResponseBody error = convertResponseToClass(result, ErrorResponseBody.class);
            assertThat(error.getType())
                    .isEqualTo(HandlerMethodValidationException.class.getSimpleName());
        }

        @Test
        @DisplayName("실패 케이스 : fileName이 blank")
        void generatePresignedUrl_blankFilename() {
            ProductPresignedUrlRequest request = new ProductPresignedUrlRequest(List.of(
                    new ProductPresignedUrlRequest.ProductRequest("", MediaType.IMAGE_JPEG_VALUE)
            ));

            MvcResult result = invoke(
                    post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(convertObjectToJson(request)),
                    status().isBadRequest(),
                    true
            );

            ErrorResponseBody error = convertResponseToClass(result, ErrorResponseBody.class);
            assertThat(error.getType())
                    .isEqualTo(HandlerMethodValidationException.class.getSimpleName());
        }

        @Test
        @DisplayName("실패 케이스 : contentType이 image/*가 아닌 경우")
        void generatePresignedUrl_invalidContentType() {
            ProductPresignedUrlRequest request = new ProductPresignedUrlRequest(List.of(
                    new ProductPresignedUrlRequest.ProductRequest("invalid.txt", MediaType.TEXT_PLAIN_VALUE)
            ));

            MvcResult result = invoke(
                    post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(convertObjectToJson(request)),
                    status().isBadRequest(),
                    true
            );

            ErrorResponseBody error = convertResponseToClass(result, ErrorResponseBody.class);
            assertThat(error.getType())
                    .isEqualTo(HandlerMethodValidationException.class.getSimpleName());
        }
    }

    @Nested
    @DisplayName("상품 조회 API")
    class ProductsFindAll {
        @Test
        @DisplayName("성공 케이스 : 상품 목록 조회")
        void findAll() {
            // given
            String expectedOldNextId = "nextId";
            int pageSize = 5;
            List<GetProductsResponse.Product> contents = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (int i = 0; i < pageSize; i++) {
                contents.add(GetProductsResponse.Product.builder()
                        .title("title" + i)
                        .price(i)
                        .id("id" + i)
                        .thumbnailUrl("thumbnailUrl" + i)
                        .tagIdList(List.of())
                        .detailedAddress("detailedAddress" + i)
                        .createAt(now)
                        .build());
            }
            GetProductsResponse expectedResponse = new GetProductsResponse(contents, 5);
            given(productService.findAll(expectedOldNextId, pageSize))
                    .willReturn(expectedResponse);

            // when then
            MockHttpServletRequestBuilder mockHttpServletRequestBuilder = get("/products")
                    .param("nextId", expectedOldNextId)
                    .param("pageSize", String.valueOf(pageSize));
            MvcResult invoke = invoke(mockHttpServletRequestBuilder, status().isOk(), true);
            GetProductsResponse actualResponse = convertResponseToClass(invoke, GetProductsResponse.class);
            assertThat(actualResponse)
                    .extracting(GetProductsResponse::getNextId, GetProductsResponse::isHasNext)
                    .containsExactly(expectedResponse.getNextId(), expectedResponse.isHasNext());
            assertThat(actualResponse.getContents())
                    .hasSize(5)
                    .usingRecursiveFieldByFieldElementComparator()
                    .isEqualTo(expectedResponse.getContents());
        }
    }

    @Nested
    @DisplayName("POST /products - register products")
    class Register {
        @Test
        @DisplayName("Should register product when valid request")
        void register_validRequest() {
            // given
            RegisterProductRequest validRequest = createRequest("title", 10.1, 11.1, "address", 1000L,
                    List.of("img1", "img2"), List.of("tag1"), ProductCondition.NEW);

            IdResponse idResponse = Mockito.mock(IdResponse.class);
            given(idResponse.getId())
                    .willReturn("id");
            given(productService.register(validRequest.toServiceRequest()))
                    .willReturn(idResponse);

            String requestJson = convertObjectToJson(validRequest);
            String responseJson = convertObjectToJson(idResponse);

            // when & then
            MockHttpServletRequestBuilder builder = post("/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson);
            invoke(builder, status().isOk(), responseJson, true);

            then(productService)
                    .should()
                    .register(validRequest.toServiceRequest());
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("invalidRequests")
        @DisplayName("Should return 400 and error details for invalid register payloads")
        void register_invalidRequest(
                String caseName,
                RegisterProductRequest req,
                String expectedMessage
        ) {
            // given
            String requestJson = convertObjectToJson(req);
            MockHttpServletRequestBuilder builder = post("/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson);

            // when
            MvcResult mvcResult = invoke(builder, status().isBadRequest(), true);

            // then
            then(productService)
                    .shouldHaveNoInteractions();
            ErrorResponseBody errorBody = convertResponseToClass(mvcResult, ErrorResponseBody.class);
            assertEquals(
                    "MethodArgumentNotValidException",
                    errorBody.getType(),
                    "Expected error type for validation failure"
            );
            assertThat(errorBody.getMessage())
                    .contains(expectedMessage);
        }

        static Stream<Arguments> invalidRequests() {
            var baseLoc = new RegisterProductRequest.LocationRequest(30.0, 50.0, "address");
            return Stream.of(
                    Arguments.of(
                            "Blank title",
                            createReq("", baseLoc, 100L, List.of("i1"), List.of("t1"), ProductCondition.NEW),
                            "title must not be blank."
                    ),
                    Arguments.of(
                            "Location null",
                            new RegisterProductRequest(
                                    "Valid", "desc", null, 100L,
                                    List.of("i1"), List.of("t1"), ProductCondition.NEW
                            ),
                            "location must not be null."
                    ),
                    Arguments.of(
                            "Latitude out of range",
                            createReq(
                                    "Valid",
                                    new RegisterProductRequest.LocationRequest(-100.0, 50.0, "addr"),
                                    100L,
                                    List.of("i1"),
                                    List.of("t1"),
                                    ProductCondition.NEW
                            ),
                            "latitude must be greater than or equal to -90.0."
                    ),
                    Arguments.of(
                            "Longitude out of range",
                            createReq(
                                    "Valid",
                                    new RegisterProductRequest.LocationRequest(30.0, 200.0, "addr"),
                                    100L,
                                    List.of("i1"),
                                    List.of("t1"),
                                    ProductCondition.NEW
                            ),
                            "longitude must be less than or equal to 180.0."
                    ),
                    Arguments.of(
                            "DetailAddress blank",
                            createReq(
                                    "Valid",
                                    new RegisterProductRequest.LocationRequest(30.0, 50.0, ""),
                                    100L,
                                    List.of("i1"),
                                    List.of("t1"),
                                    ProductCondition.NEW
                            ),
                            "detailAddress must not be blank."
                    ),
                    Arguments.of(
                            "Negative price",
                            createReq("Valid", baseLoc, -1L, List.of("i1"), List.of("t1"), ProductCondition.NEW),
                            "price must be greater than or equal to 0."
                    ),
                    Arguments.of(
                            "ImageKeys null",
                            createReq("Valid", baseLoc, 100L, null, List.of("t1"), ProductCondition.NEW),
                            "imageKeys must not be null."
                    ),
                    Arguments.of(
                            "ImageKeys empty",
                            createReq("Valid", baseLoc, 100L, List.of(), List.of("t1"), ProductCondition.NEW),
                            "imageKeys must contain between 1 and 10 items."
                    ),
                    Arguments.of(
                            "Too many tags",
                            createReq(
                                    "Valid", baseLoc, 100L, List.of("i1"),
                                    List.of("t1", "t2", "t3", "t4", "t5", "t6"), ProductCondition.NEW
                            ),
                            "tagNames must not contain more than 5 items."
                    ),
                    Arguments.of(
                            "Condition null",
                            createReq("Valid", baseLoc, 100L, List.of("i1"), List.of("t1"), null),
                            "productCondition must not be null."
                    )
            );
        }

        private static RegisterProductRequest createReq(
                String title,
                RegisterProductRequest.LocationRequest loc,
                Long price,
                List<String> images,
                List<String> tags,
                ProductCondition cond
        ) {
            return new RegisterProductRequest(title, "desc", loc, price, images, tags, cond);
        }

        private static RegisterProductRequest createRequest(
                String title,
                double lat,
                double lon,
                String addr,
                long price,
                List<String> images,
                List<String> tags,
                ProductCondition cond
        ) {
            return new RegisterProductRequest(
                    title,
                    "desc",
                    new RegisterProductRequest.LocationRequest(lat, lon, addr),
                    price,
                    images,
                    tags,
                    cond
            );
        }
    }

    @Nested
    @DisplayName("상품 삭제 API")
    class DeleteProduct {
        @Test
        @DisplayName("성공 케이스 : 상품 삭제")
        void delete() {
            // given
            String productId = "objectId";
            willDoNothing()
                    .given(productService)
                    .delete(productId);

            // when
            MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/products")
                    .param("productId", productId);
            invoke(builder, status().isOk(), true);

            // then
            verify(productService, times(1))
                    .delete(productId);
        }

        @Test
        @DisplayName("실패 케이스 : 상품 ID를 포함하지 않는 경우")
        void deleteWithoutId() {
            // when
            MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/products");
            invoke(builder, status().isBadRequest(), true);

            // then
            verify(productService, times(0))
                    .delete(any());
        }
    }
}