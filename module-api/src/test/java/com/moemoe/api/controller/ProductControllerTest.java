package com.moemoe.api.controller;

import com.moemoe.api.AbstractControllerTest;
import com.moemoe.client.aws.AwsS3Client;
import com.moemoe.client.exception.ClientRuntimeException;
import com.moemoe.core.response.IdResponse;
import com.moemoe.core.service.ProductService;
import com.moemoe.mongo.constant.ProductCondition;
import com.moemoe.mongo.entity.Product;
import com.moemoe.mongo.entity.User;
import com.moemoe.mongo.repository.ProductEntityRepository;
import com.moemoe.mongo.repository.UserEntityRepository;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class ProductControllerTest extends AbstractControllerTest {
    @SpyBean
    private ProductService productService;
    @Autowired
    private UserEntityRepository userEntityRepository;
    @Autowired
    private ProductEntityRepository productEntityRepository;
    @SpyBean
    private AwsS3Client awsS3Client;
    private static String bucketName = "test";
    @Container
    static LocalStackContainer localStackContainer = new LocalStackContainer()
            .withServices(LocalStackContainer.Service.S3);

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        S3Client s3Client = getS3Client();
        try (s3Client) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        }
        registry.add("cloud.aws.accessKey", () -> localStackContainer.getAccessKey());
        registry.add("cloud.aws.secretKey", () -> localStackContainer.getSecretKey());
        registry.add("cloud.aws.region", () -> localStackContainer.getRegion());
        registry.add("cloud.aws.bucketName", () -> bucketName);
    }

    @Nested
    @DisplayName("상품 등록 API 테스트")
    class RegisterProducts {
        @AfterEach
        void destroy() {
            productEntityRepository.deleteAll();
            userEntityRepository.deleteAll();
        }

        @Test
        @DisplayName("정상 케이스 : 상품 등록이 완료된 경우")
        void register() throws Exception {
            // given
            // save seller
            User seller = userEntityRepository.save(User.builder().name("seller").build());

            // create image list
            List<MockMultipartFile> mockMultipartFiles = getMockMultipartFiles();

            // create request
            String expectedTitle = "모에 상품 판매";
            String expectedDescription = "모에 상품 판매 설명";
            long expectedPrice = 1000L;
            ProductCondition expectedCondition = ProductCondition.NEW;
            Map<String, ? extends Serializable> location = Map.of("latitude", 10.0,
                    "longitude", 20.0,
                    "detailAddress", "123 서울시 구로구");
            Map<String, Object> request = getRequest(
                    "sellerId", seller.getId().toHexString(),
                    "title", expectedTitle,
                    "description", expectedDescription,
                    "location", location,
                    "price", expectedPrice,
                    "tagIdList", new ArrayList<>(),
                    "condition", expectedCondition
            );
            String requestJson = convertRequestToJson(request);

            // mock s3 client
            S3Client s3Client1 = getS3Client();
            given(awsS3Client.getS3Client())
                    .willReturn(s3Client1);

            // when
            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartHttpServletRequestBuilder content = multipart("/products")
                    .file(requestPart)
                    .file(mockMultipartFiles.get(0))
                    .file(mockMultipartFiles.get(1));
            ResultActions resultActions = mockMvc.perform(content)
                    .andExpect(status().isOk())
                    .andDo(print());
            String actualProductKey = convertResponseToClass(resultActions, IdResponse.class).getId();

            // then
            Product actualProduct = productEntityRepository.findById(new ObjectId(actualProductKey)).orElseThrow();
            assertThat(actualProduct)
                    .extracting(Product::getSellerId)
                    .isEqualTo(seller.getId());
            assertThat(actualProduct)
                    .extracting(Product::getTitle)
                    .isEqualTo(expectedTitle);
            assertThat(actualProduct)
                    .extracting(Product::getDescription)
                    .isEqualTo(expectedDescription);
            assertThat(actualProduct)
                    .extracting(Product::getPrice)
                    .isEqualTo(expectedPrice);
            assertThat(actualProduct)
                    .extracting(Product::getCondition)
                    .isEqualTo(expectedCondition);
            assertThat(actualProduct)
                    .extracting(Product::getLocation)
                    .extracting(Product.Location::getLatitude, Product.Location::getLongitude, Product.Location::getDetailedAddress)
                    .containsExactly(10.0, 20.0, "123 서울시 구로구");
            assertThat(actualProduct.getTagIdList())
                    .isEmpty();

            String expectedImageUrl1 = Path.of(seller.getId().toHexString(), getFileName(mockMultipartFiles.getFirst())).toString();
            String expectedImageUrl2 = Path.of(seller.getId().toHexString(), getFileName(mockMultipartFiles.getLast())).toString();
            assertThat(actualProduct.getImageUrlList())
                    .hasSize(2)
                    .containsExactlyInAnyOrder(
                            expectedImageUrl1,
                            expectedImageUrl2
                    );

            S3Client s3Client = getS3Client();
            try (s3Client) {
                ListObjectsV2Response listObjectsV2Response = s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucketName).build());
                List<S3Object> contents = listObjectsV2Response.contents();
                assertThat(contents)
                        .hasSize(2)
                        .extracting(S3Object::key)
                        .containsExactlyInAnyOrder(expectedImageUrl1, expectedImageUrl2);
            }
        }

        @Test
        @DisplayName("실패 케이스 : 판매자 아이디 데이터를 입력하지 않은 경우")
        void registerInvalidSellerId() throws Exception {
            String expectedTitle = "모에 상품 판매";
            String expectedDescription = "모에 상품 판매 설명";
            long expectedPrice = 1000L;
            ProductCondition expectedCondition = ProductCondition.NEW;
            Map<String, ? extends Serializable> location = Map.of("latitude", 10.0,
                    "longitude", 20.0,
                    "detailAddress", "123 서울시 구로구");
            Map<String, Object> request = getRequest(
                    "title", expectedTitle,
                    "description", expectedDescription,
                    "location", location,
                    "price", expectedPrice,
                    "tagIdList", new ArrayList<>(),
                    "condition", expectedCondition
            );
            String requestJson = convertRequestToJson(request);
            List<MockMultipartFile> mockMultipartFiles = getMockMultipartFiles();

            // when
            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartHttpServletRequestBuilder content = multipart("/products")
                    .file(requestPart)
                    .file(mockMultipartFiles.get(0))
                    .file(mockMultipartFiles.get(1));
            mockMvc.perform(content)
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @DisplayName("실패 케이스 : 제목 데이터를 입력하지 않은 경우")
        void registerEmptyTitle() throws Exception {
            User seller = userEntityRepository.save(User.builder().name("seller").build());
            String expectedDescription = "모에 상품 판매 설명";
            long expectedPrice = 1000L;
            ProductCondition expectedCondition = ProductCondition.NEW;
            Map<String, ? extends Serializable> location = Map.of(
                    "latitude", 10.0,
                    "longitude", 20.0,
                    "detailAddress", "123 서울시 구로구");
            Map<String, Object> request = getRequest(
                    "sellerId", seller.getId().toHexString(),
                    "description", expectedDescription,
                    "location", location,
                    "price", expectedPrice,
                    "tagIdList", new ArrayList<>(),
                    "condition", expectedCondition
            );
            String requestJson = convertRequestToJson(request);
            List<MockMultipartFile> mockMultipartFiles = getMockMultipartFiles();

            // when
            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartHttpServletRequestBuilder content = multipart("/products")
                    .file(requestPart)
                    .file(mockMultipartFiles.get(0))
                    .file(mockMultipartFiles.get(1));
            mockMvc.perform(content)
                    .andExpect(status().isBadRequest())
                    .andDo(print());

        }

        @Test
        @DisplayName("실패 케이스 : 위치 정보를 입력하지 않은 경우")
        void registerNullLocation() throws Exception {
            User seller = userEntityRepository.save(User.builder().name("seller").build());
            String expectedTitle = "모에 상품 판매";
            String expectedDescription = "모에 상품 판매 설명";
            long expectedPrice = 1000L;
            ProductCondition expectedCondition = ProductCondition.NEW;
            Map<String, Object> request = getRequest(
                    "sellerId", seller.getId().toHexString(),
                    "title", expectedTitle,
                    "description", expectedDescription,
                    "price", expectedPrice,
                    "tagIdList", new ArrayList<>(),
                    "condition", expectedCondition
            );
            String requestJson = convertRequestToJson(request);
            List<MockMultipartFile> mockMultipartFiles = getMockMultipartFiles();

            // when
            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartHttpServletRequestBuilder content = multipart("/products")
                    .file(requestPart)
                    .file(mockMultipartFiles.get(0))
                    .file(mockMultipartFiles.get(1));
            mockMvc.perform(content)
                    .andExpect(status().isBadRequest())
                    .andDo(print());

        }

        @Test
        @DisplayName("실패 케이스 : 가격이 0보다 작은 경우")
        void registerPriceLessThanZero() throws Exception {
            User seller = userEntityRepository.save(User.builder().name("seller").build());
            String expectedTitle = "모에 상품 판매";
            String expectedDescription = "모에 상품 판매 설명";
            long expectedPrice = -1000L;
            Map<String, ? extends Serializable> location = Map.of(
                    "latitude", 10.0,
                    "longitude", 20.0,
                    "detailAddress", "123 서울시 구로구");
            ProductCondition expectedCondition = ProductCondition.NEW;
            Map<String, Object> request = getRequest(
                    "sellerId", seller.getId().toHexString(),
                    "title", expectedTitle,
                    "description", expectedDescription,
                    "location", location,
                    "price", expectedPrice,
                    "tagIdList", new ArrayList<>(),
                    "condition", expectedCondition
            );
            String requestJson = convertRequestToJson(request);
            List<MockMultipartFile> mockMultipartFiles = getMockMultipartFiles();

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartHttpServletRequestBuilder content = multipart("/products")
                    .file(requestPart)
                    .file(mockMultipartFiles.get(0))
                    .file(mockMultipartFiles.get(1));
            mockMvc.perform(content)
                    .andExpect(status().isBadRequest())
                    .andDo(print());

        }

        @Test
        @DisplayName("실패 케이스 : 태그 데이터를 입력하지 않은 경우")
        void registerNullTagIdList() throws Exception {
            User seller = userEntityRepository.save(User.builder().name("seller").build());
            String expectedTitle = "모에 상품 판매";
            String expectedDescription = "모에 상품 판매 설명";
            long expectedPrice = 1000L;
            Map<String, ? extends Serializable> location = Map.of(
                    "latitude", 10.0,
                    "longitude", 20.0,
                    "detailAddress", "123 서울시 구로구");
            ProductCondition expectedCondition = ProductCondition.NEW;
            Map<String, Object> request = getRequest(
                    "sellerId", seller.getId().toHexString(),
                    "title", expectedTitle,
                    "description", expectedDescription,
                    "location", location,
                    "price", expectedPrice,
                    "condition", expectedCondition
            );
            String requestJson = convertRequestToJson(request);
            List<MockMultipartFile> mockMultipartFiles = getMockMultipartFiles();

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartHttpServletRequestBuilder content = multipart("/products")
                    .file(requestPart)
                    .file(mockMultipartFiles.get(0))
                    .file(mockMultipartFiles.get(1));
            mockMvc.perform(content)
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @DisplayName("실패 케이스 : 태그 데이터가 5개보다 많은 경우")
        void registerMaxTagIdList() throws Exception {
            User seller = userEntityRepository.save(User.builder().name("seller").build());
            String expectedTitle = "모에 상품 판매";
            String expectedDescription = "모에 상품 판매 설명";
            long expectedPrice = 1000L;
            Map<String, ? extends Serializable> location = Map.of(
                    "latitude", 10.0,
                    "longitude", 20.0,
                    "detailAddress", "123 서울시 구로구");
            ProductCondition expectedCondition = ProductCondition.NEW;
            Map<String, Object> request = getRequest(
                    "sellerId", seller.getId().toHexString(),
                    "title", expectedTitle,
                    "description", expectedDescription,
                    "location", location,
                    "price", expectedPrice,
                    "tagIdList", List.of("tag1", "tag2", "tag3", "tag4", "tag5", "tag6"),
                    "condition", expectedCondition
            );
            String requestJson = convertRequestToJson(request);
            List<MockMultipartFile> mockMultipartFiles = getMockMultipartFiles();

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartHttpServletRequestBuilder content = multipart("/products")
                    .file(requestPart)
                    .file(mockMultipartFiles.get(0))
                    .file(mockMultipartFiles.get(1));
            mockMvc.perform(content)
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @DisplayName("실패 케이스 : 이미지 데이터가 1개보다 적은 경우")
        void registerMinImageList() throws Exception {
            User seller = userEntityRepository.save(User.builder().name("seller").build());
            String expectedTitle = "모에 상품 판매";
            String expectedDescription = "모에 상품 판매 설명";
            long expectedPrice = 1000L;
            Map<String, ? extends Serializable> location = Map.of(
                    "latitude", 10.0,
                    "longitude", 20.0,
                    "detailAddress", "123 서울시 구로구");
            ProductCondition expectedCondition = ProductCondition.NEW;
            Map<String, Object> request = getRequest(
                    "sellerId", seller.getId().toHexString(),
                    "title", expectedTitle,
                    "description", expectedDescription,
                    "location", location,
                    "price", expectedPrice,
                    "tagIdList", new ArrayList<>(),
                    "condition", expectedCondition
            );
            String requestJson = convertRequestToJson(request);

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartHttpServletRequestBuilder content = multipart("/products")
                    .file(requestPart);
            mockMvc.perform(content)
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @DisplayName("실패 케이스 : 이미지 데이터가 10개보다 많은 경우")
        void registerMaxImageList() throws Exception {
            User seller = userEntityRepository.save(User.builder().name("seller").build());
            String expectedTitle = "모에 상품 판매";
            String expectedDescription = "모에 상품 판매 설명";
            long expectedPrice = 1000L;
            Map<String, ? extends Serializable> location = Map.of(
                    "latitude", 10.0,
                    "longitude", 20.0,
                    "detailAddress", "123 서울시 구로구");
            ProductCondition expectedCondition = ProductCondition.NEW;
            Map<String, Object> request = getRequest(
                    "sellerId", seller.getId().toHexString(),
                    "title", expectedTitle,
                    "description", expectedDescription,
                    "location", location,
                    "price", expectedPrice,
                    "tagIdList", new ArrayList<>(),
                    "condition", expectedCondition
            );
            String requestJson = convertRequestToJson(request);
            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));

            List<MockMultipartFile> mockMultipartFiles = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                MockMultipartFile file = new MockMultipartFile(
                        "imageList",
                        "test" + i + "jpg",
                        MediaType.IMAGE_JPEG_VALUE,
                        "Image".getBytes()
                );
                mockMultipartFiles.add(file);
            }

            MockMultipartHttpServletRequestBuilder content = multipart("/products")
                    .file(requestPart)
                    .file(mockMultipartFiles.get(0))
                    .file(mockMultipartFiles.get(1))
                    .file(mockMultipartFiles.get(2))
                    .file(mockMultipartFiles.get(3))
                    .file(mockMultipartFiles.get(4))
                    .file(mockMultipartFiles.get(5))
                    .file(mockMultipartFiles.get(6))
                    .file(mockMultipartFiles.get(7))
                    .file(mockMultipartFiles.get(8))
                    .file(mockMultipartFiles.get(9))
                    .file(mockMultipartFiles.get(10));
            mockMvc.perform(content)
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }


        @Test
        @DisplayName("실패 케이스 : 상태 데이터를 입력하지 않은 경우 경우")
        void registerNullCondition() throws Exception {
            User seller = userEntityRepository.save(User.builder().name("seller").build());
            String expectedTitle = "모에 상품 판매";
            String expectedDescription = "모에 상품 판매 설명";
            long expectedPrice = 1000L;
            Map<String, ? extends Serializable> location = Map.of(
                    "latitude", 10.0,
                    "longitude", 20.0,
                    "detailAddress", "123 서울시 구로구");
            Map<String, Object> request = getRequest(
                    "sellerId", seller.getId().toHexString(),
                    "title", expectedTitle,
                    "description", expectedDescription,
                    "location", location,
                    "price", expectedPrice,
                    "tagIdList", new ArrayList<>()
            );
            String requestJson = convertRequestToJson(request);
            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));

            List<MockMultipartFile> mockMultipartFiles = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                MockMultipartFile file = new MockMultipartFile(
                        "imageList",
                        "test" + i + "jpg",
                        MediaType.IMAGE_JPEG_VALUE,
                        "Image".getBytes()
                );
                mockMultipartFiles.add(file);
            }

            MockMultipartHttpServletRequestBuilder content = multipart("/products")
                    .file(requestPart)
                    .file(mockMultipartFiles.get(0))
                    .file(mockMultipartFiles.get(1))
                    .file(mockMultipartFiles.get(2))
                    .file(mockMultipartFiles.get(3))
                    .file(mockMultipartFiles.get(4))
                    .file(mockMultipartFiles.get(5))
                    .file(mockMultipartFiles.get(6))
                    .file(mockMultipartFiles.get(7))
                    .file(mockMultipartFiles.get(8))
                    .file(mockMultipartFiles.get(9))
                    .file(mockMultipartFiles.get(10));
            mockMvc.perform(content)
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @DisplayName("실패 케이스 : InvalidArgumentException 이 발생하는 케이스")
        void registerInvalidArgumentException() throws Exception {
            // save seller
            User seller = userEntityRepository.save(User.builder().name("seller").build());

            // create image list
            List<MockMultipartFile> mockMultipartFiles = getMockMultipartFiles();

            // create request
            String expectedTitle = "모에 상품 판매";
            String expectedDescription = "모에 상품 판매 설명";
            long expectedPrice = 1000L;
            Map<String, ? extends Serializable> location = Map.of("latitude", 10.0,
                    "longitude", 20.0,
                    "detailAddress", "123 서울시 구로구");
            ProductCondition expectedCondition = ProductCondition.NEW;
            Map<String, Object> request = getRequest(
                    "sellerId", seller.getId().toHexString(),
                    "title", expectedTitle,
                    "description", expectedDescription,
                    "location", location,
                    "price", expectedPrice,
                    "tagIdList", new ArrayList<>(),
                    "condition", expectedCondition
            );
            String requestJson = convertRequestToJson(request);

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartHttpServletRequestBuilder content = multipart("/products")
                    .file(requestPart)
                    .file(mockMultipartFiles.get(0))
                    .file(mockMultipartFiles.get(1));
            doThrow(new IllegalArgumentException("Runtime Exception")).when(productService).register(any(), any());

            // when
            mockMvc.perform(content)
                    .andExpect(status().isInternalServerError())
                    .andDo(print());
        }

        @Test
        @DisplayName("실패 케이스 : ClientRuntimeException 이 발생하는 케이스")
        void registerClientRuntimeException() throws Exception {
            // save seller
            User seller = userEntityRepository.save(User.builder().name("seller").build());

            // create image list
            List<MockMultipartFile> mockMultipartFiles = getMockMultipartFiles();

            // create request
            String expectedTitle = "모에 상품 판매";
            String expectedDescription = "모에 상품 판매 설명";
            long expectedPrice = 1000L;
            Map<String, ? extends Serializable> location = Map.of("latitude", 10.0,
                    "longitude", 20.0,
                    "detailAddress", "123 서울시 구로구");
            ProductCondition expectedCondition = ProductCondition.NEW;
            Map<String, Object> request = getRequest(
                    "sellerId", seller.getId().toHexString(),
                    "title", expectedTitle,
                    "description", expectedDescription,
                    "location", location,
                    "price", expectedPrice,
                    "tagIdList", new ArrayList<>(),
                    "condition", expectedCondition
            );
            String requestJson = convertRequestToJson(request);

            MockMultipartFile requestPart = new MockMultipartFile("request",
                    "request",
                    MediaType.APPLICATION_JSON_VALUE,
                    requestJson.getBytes(StandardCharsets.UTF_8));
            MockMultipartHttpServletRequestBuilder content = multipart("/products")
                    .file(requestPart)
                    .file(mockMultipartFiles.get(0))
                    .file(mockMultipartFiles.get(1));
            doThrow(new ClientRuntimeException("Client Runtime Exception")).when(productService).register(any(), any());

            // when
            mockMvc.perform(content)
                    .andExpect(status().isInternalServerError())
                    .andDo(print());
        }

        private @NotNull List<MockMultipartFile> getMockMultipartFiles() throws IOException {
            List<MockMultipartFile> mockMultipartFiles = new ArrayList<>();
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:image/*.{png,jpeg}");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String contentType = Files.probeContentType(resource.getFile().toPath());
                MockMultipartFile file = new MockMultipartFile(
                        "imageList",
                        filename,
                        contentType,
                        resource.getInputStream()
                );
                mockMultipartFiles.add(file);
            }
            return mockMultipartFiles;
        }


        private String getFileName(MultipartFile image) {
            return Optional.ofNullable(image.getOriginalFilename())
                    .map(fileName -> Path.of(fileName).getFileName().toString())
                    .orElseThrow(() -> new IllegalArgumentException("파일 이름이 null 입니다."));
        }
    }

    private static S3Client getS3Client() {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(
                localStackContainer.getAccessKey(),
                localStackContainer.getSecretKey()
        );
        Region region = Region.of(localStackContainer.getRegion());
        return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .endpointOverride(localStackContainer.getEndpoint())
                .region(region)
                .build();
    }
}