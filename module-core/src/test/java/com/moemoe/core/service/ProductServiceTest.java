package com.moemoe.core.service;

import com.moemoe.client.aws.AwsS3Client;
import com.moemoe.core.request.RegisterProductServiceRequest;
import com.moemoe.core.response.GetProductsResponse;
import com.moemoe.core.response.IdResponse;
import com.moemoe.core.security.MoeUser;
import com.moemoe.core.service.resolver.ProductImageKeyResolver;
import com.moemoe.mongo.constant.ProductCondition;
import com.moemoe.mongo.constant.UserRole;
import com.moemoe.mongo.entity.ProductEntity;
import com.moemoe.mongo.entity.TagEntity;
import com.moemoe.mongo.repository.ProductEntityRepository;
import com.moemoe.mongo.repository.TagEntityRepository;
import com.moemoe.mongo.repository.UserEntityRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @InjectMocks
    private ProductService productService;
    @Mock
    private UserEntityRepository userEntityRepository;
    @Mock
    private ProductEntityRepository productEntityRepository;
    @Mock
    private TagEntityRepository tagEntityRepository;
    @Mock
    private TagService tagService;
    @Mock
    private AwsS3Client awsS3Client;
    @Mock
    private ProductImageKeyResolver imageKeyResolver;
    private ObjectId expectedSellerId;

    @BeforeEach
    void setUp() {
        MoeUser moeUser = MoeUser.of("64a9ef6a8ed14e3b1c8eb29a", "moemoe@example.com", UserRole.USER);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                moeUser,
                null,
                null
        );
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        expectedSellerId = new ObjectId("64a9ef6a8ed14e3b1c8eb29a");
    }

    @Nested
    @DisplayName("Register product")
    class Register {
        @Test
        @DisplayName("Should register product successfully when seller exists and request is valid")
        void registerProduct_success() {
            // given
            RegisterProductServiceRequest request = new RegisterProductServiceRequest(
                    "상품 제목",
                    "상품 설명",
                    10000,
                    List.of("tag1", "tag2"),
                    List.of("fileKey1", "fileKey2"),
                    new RegisterProductServiceRequest.LocationDto(10.0, 11.0, "detail address"),
                    ProductCondition.NEW
            );
            // validateSellerExists
            given(userEntityRepository.existsById(expectedSellerId))
                    .willReturn(true);

            // save
            ProductEntity entity = request.toEntity(expectedSellerId);
            ProductEntity savedEntity = BDDMockito.mock(ProductEntity.class);
            ObjectId expectedId = new ObjectId();
            given(savedEntity.getId())
                    .willReturn(expectedId);
            given(productEntityRepository.save(entity))
                    .willReturn(savedEntity);

            // when
            IdResponse response = productService.register(request);

            // then
            then(userEntityRepository)
                    .should()
                    .existsById(expectedSellerId);
            then(tagService)
                    .should()
                    .incrementProductsCount(request.tagNames());
            then(productEntityRepository)
                    .should()
                    .save(entity);

            assertThat(response.getId())
                    .isEqualTo(expectedId.toHexString());
        }

        @Test
        @DisplayName("Should throw exception when seller ID does not exist")
        void registerProduct_invalidSellerId() {
            // given
            RegisterProductServiceRequest request = new RegisterProductServiceRequest(
                    "상품 제목",
                    "상품 설명",
                    10000,
                    List.of("tag1", "tag2"),
                    List.of("fileKey1", "fileKey2"),
                    new RegisterProductServiceRequest.LocationDto(10.0, 11.0, "detail address"),
                    ProductCondition.NEW
            );
            given(userEntityRepository.existsById(expectedSellerId))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> productService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Seller with ID");

            then(userEntityRepository)
                    .should()
                    .existsById(expectedSellerId);
            then(tagService)
                    .shouldHaveNoInteractions();
            then(productEntityRepository)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Should throw exception when saving product entity fails")
        void registerProduct_dbSaveFails() {
            // given
            RegisterProductServiceRequest request = new RegisterProductServiceRequest(
                    "상품 제목",
                    "상품 설명",
                    10000,
                    List.of("tag1", "tag2"),
                    List.of("fileKey1", "fileKey2"),
                    new RegisterProductServiceRequest.LocationDto(10.0, 11.0, "detail address"),
                    ProductCondition.NEW
            );
            given(userEntityRepository.existsById(expectedSellerId))
                    .willReturn(true);
            willThrow(new RuntimeException("DB error"))
                    .given(productEntityRepository)
                    .save(any(ProductEntity.class));

            // when & then
            assertThatThrownBy(() -> productService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB error");

            then(userEntityRepository)
                    .should()
                    .existsById(expectedSellerId);
            then(tagService)
                    .should()
                    .incrementProductsCount(request.tagNames());
            then(productEntityRepository)
                    .should()
                    .save(any(ProductEntity.class));
        }
    }

    @Nested
    @DisplayName("Find All by Paging")
    class FindAllByPaging {
        @Test
        @DisplayName("Should return first page and hasNext=true when repository returns more than pageSize items")
        void shouldReturnFirstPageAndHasNextTrueWhenMoreThanPageSize() {
            // given
            ProductEntity e1 = getProductEntity(new ObjectId().toHexString(), "title1", List.of("tag1"),
                    "thumb/1.jpg", "detail1", 100L, now(), now());
            ProductEntity e2 = getProductEntity(new ObjectId().toHexString(), "title2", List.of("tag1", " tag2"),
                    "thumb/2.jpg", "detail2", 200L, now(), now());
            ProductEntity e3 = getProductEntity(new ObjectId().toHexString(), "title3", List.of("tag3"),
                    "thumb/3.jpg", "detail3", 300L, now(), now());
            List<ProductEntity> productEntities = List.of(e1, e2, e3);

            given(imageKeyResolver.resolve("thumb/1.jpg")).willReturn("http://cdn/thumbnail/thumb/1.jpg");
            given(imageKeyResolver.resolve("thumb/2.jpg")).willReturn("http://cdn/thumbnail/thumb/2.jpg");
            given(imageKeyResolver.resolve("thumb/3.jpg")).willReturn("http://cdn/thumbnail/thumb/3.jpg");

            String expectedOldNextId = new ObjectId().toHexString();
            int expectedPageSize = 2;
            given(productEntityRepository.findPage(expectedOldNextId, expectedPageSize))
                    .willReturn(productEntities);

            // when
            GetProductsResponse actual = productService.findAll(expectedOldNextId, expectedPageSize);

            // then
            assertThat(actual)
                    .extracting(GetProductsResponse::getNextId, GetProductsResponse::isHasNext)
                    .containsExactly(productEntities.get(expectedPageSize - 1).getStringId(), true);
            assertThat(actual.getContents())
                    .hasSize(expectedPageSize)
                    .extracting(
                            GetProductsResponse.Product::id,
                            GetProductsResponse.Product::title,
                            GetProductsResponse.Product::tagNames,
                            GetProductsResponse.Product::thumbnailUrl,
                            GetProductsResponse.Product::detailedAddress,
                            GetProductsResponse.Product::price,
                            GetProductsResponse.Product::createdAt,
                            GetProductsResponse.Product::updatedAt
                    )
                    .containsExactly(
                            tuple(e1.getStringId(), e1.getTitle(), e1.getTagNames(),
                                    "http://cdn/thumbnail/thumb/1.jpg", e1.getDetailedAddress(), e1.getPrice(), e1.getCreatedAt(), e1.getUpdatedAt()),
                            tuple(e2.getStringId(), e2.getTitle(), e2.getTagNames(),
                                    "http://cdn/thumbnail/thumb/2.jpg", e2.getDetailedAddress(), e2.getPrice(), e2.getCreatedAt(), e2.getUpdatedAt())
                    );

            then(productEntityRepository).should()
                    .findPage(expectedOldNextId, expectedPageSize);
            then(imageKeyResolver).should(times(expectedPageSize + 1))
                    .resolve(anyString());
        }

        @Test
        @DisplayName("Should return last page and hasNext=false when repository returns exactly pageSize items and nextId is blank")
        void shouldReturnLastPageAndHasNextFalseWhenExactlyPageSizeAndBlankNextId() {
            // given
            int expectedPageSize = 2;
            ProductEntity e1 = getProductEntity(new ObjectId().toHexString(), "title1", List.of("tag1"),
                    "thumb/A.jpg", "detail1", 100L, now(), now());
            ProductEntity e2 = getProductEntity(new ObjectId().toHexString(), "title2", List.of("tag1", " tag2"),
                    "thumb/B.jpg", "detail2", 200L, now(), now());
            List<ProductEntity> productEntities = List.of(e1, e2);

            given(imageKeyResolver.resolve("thumb/A.jpg")).willReturn("http://cdn/thumbnail/thumb/A.jpg");
            given(imageKeyResolver.resolve("thumb/B.jpg")).willReturn("http://cdn/thumbnail/thumb/B.jpg");

            given(productEntityRepository.findPage("", expectedPageSize))
                    .willReturn(productEntities);

            // when
            GetProductsResponse actual = productService.findAll("", expectedPageSize);

            // then
            assertThat(actual)
                    .extracting(GetProductsResponse::getNextId, GetProductsResponse::isHasNext)
                    .containsExactly("", false);

            assertThat(actual.getContents())
                    .hasSize(expectedPageSize)
                    .extracting(
                            GetProductsResponse.Product::id,
                            GetProductsResponse.Product::title,
                            GetProductsResponse.Product::tagNames,
                            GetProductsResponse.Product::thumbnailUrl,
                            GetProductsResponse.Product::detailedAddress,
                            GetProductsResponse.Product::price,
                            GetProductsResponse.Product::createdAt,
                            GetProductsResponse.Product::updatedAt
                    )
                    .containsExactly(
                            tuple(e1.getStringId(), e1.getTitle(), e1.getTagNames(),
                                    "http://cdn/thumbnail/thumb/A.jpg", e1.getDetailedAddress(), e1.getPrice(), e1.getCreatedAt(), e1.getUpdatedAt()),
                            tuple(e2.getStringId(), e2.getTitle(), e2.getTagNames(),
                                    "http://cdn/thumbnail/thumb/B.jpg", e2.getDetailedAddress(), e2.getPrice(), e2.getCreatedAt(), e2.getUpdatedAt())
                    );

            then(productEntityRepository).should().findPage("", expectedPageSize);
            then(imageKeyResolver).should(times(expectedPageSize)).resolve(anyString());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when nextId is invalid hex string")
        void shouldThrowWhenNextIdIsInvalidHex() {
            // given
            String invalidNextId = "not-a-hex";

            // when / then
            assertThatThrownBy(() -> productService.findAll(invalidNextId, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invalid hex ObjectId");

            then(productEntityRepository).shouldHaveNoInteractions();
            then(imageKeyResolver).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Should use default pageSize when requestedPageSize is non-positive")
        void shouldUseDefaultPageSizeWhenRequestedSizeIsNonPositive() {
            // given
            given(productEntityRepository.findPage(any(), anyInt()))
                    .willAnswer(inv -> {
                        int ps = inv.getArgument(1, Integer.class);
                        return buildEntities(ps); // 🔹 각 엔티티에 서로 다른 thumbnail 키를 심어줌
                    });

            // 각 key에 대한 resolver 스텁(간단히 anyString → prefix 붙이기)
            willAnswer(inv -> "http://cdn/thumbnail/" + inv.getArgument(0))
                    .given(imageKeyResolver).resolve(anyString());

            // when
            GetProductsResponse actual = productService.findAll("", 0);

            // then
            assertThat(actual.isHasNext()).isFalse();

            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            then(productEntityRepository).should().findPage(eq(""), captor.capture());
            Integer normalized = captor.getValue();

            assertThat(normalized).isNotNull().isPositive();
            assertThat(actual.getContents()).hasSize(normalized);
            then(imageKeyResolver).should(times(normalized)).resolve(anyString());
        }

        @Test
        @DisplayName("Should cap pageSize to max when requestedPageSize exceeds limit")
        void shouldCapPageSizeWhenRequestedSizeExceedsLimit() {
            // given
            given(productEntityRepository.findPage(any(), anyInt()))
                    .willAnswer(inv -> {
                        int ps = inv.getArgument(1, Integer.class);
                        return buildEntities(ps + 1); // hasNext=true 유도
                    });
            willAnswer(inv -> "http://cdn/thumbnail/" + inv.getArgument(0))
                    .given(imageKeyResolver).resolve(anyString());

            int huge = Integer.MAX_VALUE;

            // when
            GetProductsResponse actual = productService.findAll("", huge);

            // then
            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            then(productEntityRepository).should().findPage(eq(""), captor.capture());
            Integer normalized = captor.getValue();

            assertThat(normalized).isNotNull().isPositive().isLessThan(huge);
            assertThat(actual.isHasNext()).isTrue();
            assertThat(actual.getContents()).hasSize(normalized);
            then(imageKeyResolver).should(times(normalized + 1)).resolve(anyString());
        }

        // ===== helpers =====

        private List<ProductEntity> buildEntities(int count) {
            List<ProductEntity> list = new ArrayList<>();
            LocalDateTime now = now();
            for (int i = 0; i < count; i++) {
                list.add(getProductEntity(new ObjectId().toHexString(),
                        "title" + i,
                        List.of("tag" + i),
                        "thumb/" + i + ".jpg",   // 🔹 서로 다른 썸네일 키
                        "detail" + i,
                        100L + i,
                        now, now));
            }
            return list;
        }

        private LocalDateTime now() {
            return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        }

        private ProductEntity getProductEntity(String id, String title, List<String> tagNames,
                                               String thumbnailKey, String detailedAddress, Long price,
                                               LocalDateTime createdAt, LocalDateTime updatedAt) {
            ProductEntity mock = BDDMockito.mock(ProductEntity.class);
            given(mock.getStringId()).willReturn(id);
            given(mock.getTitle()).willReturn(title);
            given(mock.getTagNames()).willReturn(tagNames);
            // 🔹 썸네일 키
            given(mock.getThumbnailUrl()).willReturn(thumbnailKey);
            given(mock.getDetailedAddress()).willReturn(detailedAddress);
            given(mock.getPrice()).willReturn(price);
            given(mock.getCreatedAt()).willReturn(createdAt);
            given(mock.getUpdatedAt()).willReturn(updatedAt);
            return mock;
        }
    }

    @Test
    @DisplayName("성공 케이스 : 상품 삭제")
    void delete() {
        // given
        ObjectId productObjectId = new ObjectId();
        List<String> imageUrlList = List.of("imageUrl1", "imageUrl2");
        List<String> tagNameList = List.of("tag1");
        ProductEntity productEntity = ProductEntity.of(
                new ObjectId(),
                "",
                "",
                ProductEntity.Location.of(10.0, 10.0, ""),
                1000L,
                imageUrlList,
                tagNameList,
                ProductCondition.HEAVY_SIGNS_OF_USE
        );

        given(productEntityRepository.findById(productObjectId))
                .willReturn(Optional.of(productEntity));
        TagEntity tagEntity = TagEntity.of("tag1", 1L);
        List<TagEntity> tagEntityList = List.of(tagEntity);
        given(tagEntityRepository.findAllByNameIn(tagNameList))
                .willReturn(tagEntityList);
        willDoNothing()
                .given(awsS3Client)
                .delete(imageUrlList);

        // when
        String productId = productObjectId.toHexString();
        productService.delete(productId);

        // then
        verify(productEntityRepository, times(1))
                .findById(productObjectId);
        verify(tagEntityRepository, times(1))
                .findAllByNameIn(tagNameList);
        verify(awsS3Client, times(1))
                .delete(imageUrlList);
        verify(productEntityRepository, times(1))
                .delete(productEntity);
    }
}