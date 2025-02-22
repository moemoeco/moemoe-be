package com.moemoe.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moemoe.api.AbstractControllerTest;
import com.moemoe.api.config.handler.ErrorResponseBody;
import com.moemoe.client.exception.ClientRuntimeException;
import com.moemoe.core.request.RegisterProductRequest;
import com.moemoe.core.response.GetProductsResponse;
import com.moemoe.core.response.IdResponse;
import com.moemoe.core.service.ProductService;
import com.moemoe.mongo.constant.ProductCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest extends AbstractControllerTest {
    @MockBean
    private ProductService productService;

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
    @DisplayName("상품 등록 API")
    class RegisterProducts {
        private String mockRequest(RegisterProductRequest request) throws JsonProcessingException {
            double expectedLatitude = 10.1;
            double expectedLongitude = 11.1;
            String expectedDetailedAddress = "address";
            String expectedSellerId = "507f1f77bcf86cd799439011";
            String expectedTitle = "title";
            String expectedDescription = "description";
            long expectedPrice = 1000L;
            List<String> expectedTagNameList = List.of("tag1");
            ProductCondition expectedCondition = ProductCondition.NEW;
            RegisterProductRequest.Location expectedLocation = new RegisterProductRequest.Location();
            ReflectionTestUtils.setField(expectedLocation, "latitude", expectedLatitude);
            ReflectionTestUtils.setField(expectedLocation, "longitude", expectedLongitude);
            ReflectionTestUtils.setField(expectedLocation, "detailAddress", expectedDetailedAddress);

            ReflectionTestUtils.setField(request, "sellerId", expectedSellerId);
            ReflectionTestUtils.setField(request, "title", expectedTitle);
            ReflectionTestUtils.setField(request, "description", expectedDescription);
            ReflectionTestUtils.setField(request, "price", expectedPrice);
            ReflectionTestUtils.setField(request, "tagNameList", expectedTagNameList);
            ReflectionTestUtils.setField(request, "condition", expectedCondition);
            ReflectionTestUtils.setField(request, "location", expectedLocation);

            String requestJson = convertRequestToJson(request);

            ObjectNode objectNode = (ObjectNode) objectMapper.readTree(requestJson);
            objectNode.put("sellerId", expectedSellerId);
            return objectMapper.writeValueAsString(objectNode);
        }

        @Test
        @DisplayName("정상 케이스 : 상품 등록이 완료된 경우")
        void register() throws Exception {
            // given
            RegisterProductRequest request = new RegisterProductRequest();
            String requestJson = mockRequest(request);

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartFile file = new MockMultipartFile(
                    "imageList",
                    "test1",
                    "image/png",
                    "abcd".getBytes()
            );
            MockHttpServletRequestBuilder builder = multipart("/products")
                    .file(requestPart)
                    .file(file);

            IdResponse response = new IdResponse();
            String productId = "productId";
            ReflectionTestUtils.setField(response, "id", productId);
            given(productService.register(request, List.of(file)))
                    .willReturn(response);

            // when then
            MvcResult invoke = invoke(builder, status().isOk(), true);
            IdResponse actualResponse = convertResponseToClass(invoke, IdResponse.class);
            assertThat(actualResponse)
                    .isEqualTo(response);
        }

        @Test
        @DisplayName("실패 케이스 : Seller Id를 입력하지 않은 경우")
        void registerEmptySellerId() throws Exception {
            // given
            RegisterProductRequest request = new RegisterProductRequest();
            String requestJson = mockRequest(request);

            // empty seller id
            ObjectNode objectNode = (ObjectNode) objectMapper.readTree(requestJson);
            objectNode.put("sellerId", "");
            requestJson = objectMapper.writeValueAsString(objectNode);

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartFile file = new MockMultipartFile(
                    "imageList",
                    "test1",
                    "image/png",
                    "abcd".getBytes()
            );
            MockHttpServletRequestBuilder builder = multipart("/products")
                    .file(requestPart)
                    .file(file);

            IdResponse response = new IdResponse();
            String productId = "productId";
            ReflectionTestUtils.setField(response, "id", productId);
            given(productService.register(request, List.of(file)))
                    .willReturn(response);

            // when then
            MvcResult invoke = invoke(builder, status().isBadRequest(), true);
            ErrorResponseBody errorResponseBody = convertResponseToClass(invoke, ErrorResponseBody.class);
            assertThat(errorResponseBody)
                    .extracting(ErrorResponseBody::getType)
                    .isEqualTo(HandlerMethodValidationException.class.getSimpleName());
        }

        @Test
        @DisplayName("실패 케이스 : 제목 데이터를 입력하지 않은 경우")
        void registerEmptyTitle() throws Exception {
            // given
            RegisterProductRequest request = new RegisterProductRequest();
            String requestJson = mockRequest(request);

            // empty title
            ObjectNode objectNode = (ObjectNode) objectMapper.readTree(requestJson);
            objectNode.put("title", "");
            requestJson = objectMapper.writeValueAsString(objectNode);

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartFile file = new MockMultipartFile(
                    "imageList",
                    "test1",
                    "image/png",
                    "abcd".getBytes()
            );
            MockHttpServletRequestBuilder builder = multipart("/products")
                    .file(requestPart)
                    .file(file);

            IdResponse response = new IdResponse();
            String productId = "productId";
            ReflectionTestUtils.setField(response, "id", productId);
            given(productService.register(request, List.of(file)))
                    .willReturn(response);

            // when then
            MvcResult invoke = invoke(builder, status().isBadRequest(), true);
            ErrorResponseBody errorResponseBody = convertResponseToClass(invoke, ErrorResponseBody.class);
            assertThat(errorResponseBody)
                    .extracting(ErrorResponseBody::getType)
                    .isEqualTo(HandlerMethodValidationException.class.getSimpleName());
        }

        @Test
        @DisplayName("실패 케이스 : 위치 정보를 입력하지 않은 경우")
        void registerNullLocation() throws Exception {
            // given
            RegisterProductRequest request = new RegisterProductRequest();
            String requestJson = mockRequest(request);

            // empty location
            ObjectNode objectNode = (ObjectNode) objectMapper.readTree(requestJson);
            objectNode.remove("location");
            requestJson = objectMapper.writeValueAsString(objectNode);

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartFile file = new MockMultipartFile(
                    "imageList",
                    "test1",
                    "image/png",
                    "abcd".getBytes()
            );
            MockHttpServletRequestBuilder builder = multipart("/products")
                    .file(requestPart)
                    .file(file);

            IdResponse response = new IdResponse();
            String productId = "productId";
            ReflectionTestUtils.setField(response, "id", productId);
            given(productService.register(request, List.of(file)))
                    .willReturn(response);


            // when then
            MvcResult invoke = invoke(builder, status().isBadRequest(), true);
            ErrorResponseBody errorResponseBody = convertResponseToClass(invoke, ErrorResponseBody.class);
            assertThat(errorResponseBody)
                    .extracting(ErrorResponseBody::getType)
                    .isEqualTo(HandlerMethodValidationException.class.getSimpleName());
        }


        @Test
        @DisplayName("실패 케이스 : 가격이 0보다 작은 경우")
        void registerPriceLessThanZero() throws Exception {
            // given
            RegisterProductRequest request = new RegisterProductRequest();
            String requestJson = mockRequest(request);

            // price 가 0보다 작은 경우
            ObjectNode objectNode = (ObjectNode) objectMapper.readTree(requestJson);
            objectNode.put("price", -1);
            requestJson = objectMapper.writeValueAsString(objectNode);

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartFile file = new MockMultipartFile(
                    "imageList",
                    "test1",
                    "image/png",
                    "abcd".getBytes()
            );
            MockHttpServletRequestBuilder builder = multipart("/products")
                    .file(requestPart)
                    .file(file);

            IdResponse response = new IdResponse();
            String productId = "productId";
            ReflectionTestUtils.setField(response, "id", productId);
            given(productService.register(request, List.of(file)))
                    .willReturn(response);


            // when then
            MvcResult invoke = invoke(builder, status().isBadRequest(), true);
            ErrorResponseBody errorResponseBody = convertResponseToClass(invoke, ErrorResponseBody.class);
            assertThat(errorResponseBody)
                    .extracting(ErrorResponseBody::getType)
                    .isEqualTo(HandlerMethodValidationException.class.getSimpleName());
        }

        @Test
        @DisplayName("실패 케이스 : 태그 데이터를 입력하지 않은 경우")
        void registerNullTagIdList() throws Exception {
            // given
            RegisterProductRequest request = new RegisterProductRequest();
            String requestJson = mockRequest(request);

            // empty tag id list
            ObjectNode objectNode = (ObjectNode) objectMapper.readTree(requestJson);
            objectNode.remove("tagNameList");
            requestJson = objectMapper.writeValueAsString(objectNode);

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartFile file = new MockMultipartFile(
                    "imageList",
                    "test1",
                    "image/png",
                    "abcd".getBytes()
            );
            MockHttpServletRequestBuilder builder = multipart("/products")
                    .file(requestPart)
                    .file(file);

            IdResponse response = new IdResponse();
            String productId = "productId";
            ReflectionTestUtils.setField(response, "id", productId);
            given(productService.register(request, List.of(file)))
                    .willReturn(response);


            // when then
            MvcResult invoke = invoke(builder, status().isBadRequest(), true);
            ErrorResponseBody errorResponseBody = convertResponseToClass(invoke, ErrorResponseBody.class);
            assertThat(errorResponseBody)
                    .extracting(ErrorResponseBody::getType)
                    .isEqualTo(HandlerMethodValidationException.class.getSimpleName());
        }

        @Test
        @DisplayName("실패 케이스 : 태그 데이터가 5개보다 많은 경우")
        void registerMaxTagIdList() throws Exception {
            // given
            RegisterProductRequest request = new RegisterProductRequest();
            String requestJson = mockRequest(request);

            // tag id list 6개
            ObjectNode objectNode = (ObjectNode) objectMapper.readTree(requestJson);
            objectNode.putArray("tagNameList")
                    .add("tag1")
                    .add("tag2")
                    .add("tag3")
                    .add("tag4")
                    .add("tag5")
                    .add("tag6");
            requestJson = objectMapper.writeValueAsString(objectNode);

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartFile file = new MockMultipartFile(
                    "imageList",
                    "test1",
                    "image/png",
                    "abcd".getBytes()
            );
            MockHttpServletRequestBuilder builder = multipart("/products")
                    .file(requestPart)
                    .file(file);

            IdResponse response = new IdResponse();
            String productId = "productId";
            ReflectionTestUtils.setField(response, "id", productId);
            given(productService.register(request, List.of(file)))
                    .willReturn(response);

            // when then
            MvcResult invoke = invoke(builder, status().isBadRequest(), true);
            ErrorResponseBody errorResponseBody = convertResponseToClass(invoke, ErrorResponseBody.class);
            assertThat(errorResponseBody)
                    .extracting(ErrorResponseBody::getType)
                    .isEqualTo(HandlerMethodValidationException.class.getSimpleName());
        }

        @Test
        @DisplayName("실패 케이스 : 이미지 데이터가 10개보다 많은 경우")
        void registerMaxImageList() throws Exception {
            // given
            RegisterProductRequest request = new RegisterProductRequest();
            String requestJson = mockRequest(request);

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));

            List<MultipartFile> files = new ArrayList<>();
            for (int i = 0; i <= 10; i++) {
                files.add(new MockMultipartFile(
                        "imageList",
                        "test" + i,
                        "image/png",
                        "abcd".getBytes()
                ));
            }
            MockHttpServletRequestBuilder builder = multipart("/products")
                    .file(requestPart)
                    .file((MockMultipartFile) files.get(0))
                    .file((MockMultipartFile) files.get(1))
                    .file((MockMultipartFile) files.get(2))
                    .file((MockMultipartFile) files.get(3))
                    .file((MockMultipartFile) files.get(4))
                    .file((MockMultipartFile) files.get(5))
                    .file((MockMultipartFile) files.get(6))
                    .file((MockMultipartFile) files.get(7))
                    .file((MockMultipartFile) files.get(8))
                    .file((MockMultipartFile) files.get(9))
                    .file((MockMultipartFile) files.get(10));

            IdResponse response = new IdResponse();
            String productId = "productId";
            ReflectionTestUtils.setField(response, "id", productId);
            given(productService.register(request, files))
                    .willReturn(response);


            // when then
            MvcResult invoke = invoke(builder, status().isBadRequest(), true);
            ErrorResponseBody errorResponseBody = convertResponseToClass(invoke, ErrorResponseBody.class);
            assertThat(errorResponseBody)
                    .extracting(ErrorResponseBody::getType)
                    .isEqualTo(HandlerMethodValidationException.class.getSimpleName());
        }


        @Test
        @DisplayName("실패 케이스 : InvalidArgumentException 이 발생하는 케이스")
        void registerInvalidArgumentException() throws Exception {
            // given
            RegisterProductRequest request = new RegisterProductRequest();
            String requestJson = mockRequest(request);

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartFile file = new MockMultipartFile(
                    "imageList",
                    "test1",
                    "image/png",
                    "abcd".getBytes()
            );
            MockHttpServletRequestBuilder builder = multipart("/products")
                    .file(requestPart)
                    .file(file);

            IdResponse response = new IdResponse();
            String productId = "productId";
            ReflectionTestUtils.setField(response, "id", productId);
            given(productService.register(any(), any()))
                    .willThrow(IllegalArgumentException.class);

            // when
            MvcResult invoke = invoke(builder, status().isInternalServerError(), true);
            ErrorResponseBody errorResponseBody = convertResponseToClass(invoke, ErrorResponseBody.class);
            assertThat(errorResponseBody)
                    .extracting(ErrorResponseBody::getType)
                    .isEqualTo(IllegalArgumentException.class.getSimpleName());
        }

        @Test
        @DisplayName("실패 케이스 : ClientRuntimeException 이 발생하는 케이스")
        void registerClientRuntimeException() throws Exception {
            // given
            RegisterProductRequest request = new RegisterProductRequest();
            String requestJson = mockRequest(request);

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartFile file = new MockMultipartFile(
                    "imageList",
                    "test1",
                    "image/png",
                    "abcd".getBytes()
            );
            MockHttpServletRequestBuilder builder = multipart("/products")
                    .file(requestPart)
                    .file(file);

            IdResponse response = new IdResponse();
            String productId = "productId";
            ReflectionTestUtils.setField(response, "id", productId);
            given(productService.register(any(), any()))
                    .willThrow(ClientRuntimeException.class);


            // when then
            MvcResult invoke = invoke(builder, status().isInternalServerError(), true);
            ErrorResponseBody errorResponseBody = convertResponseToClass(invoke, ErrorResponseBody.class);
            assertThat(errorResponseBody)
                    .extracting(ErrorResponseBody::getType)
                    .isEqualTo(ClientRuntimeException.class.getSimpleName());
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
        void deleteWithoutId(){
            // when
            MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/products");
            invoke(builder, status().isBadRequest(), true);

            // then
            verify(productService, times(0))
                    .delete(any());
        }
    }
}