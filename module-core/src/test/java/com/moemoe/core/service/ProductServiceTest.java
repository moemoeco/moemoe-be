package com.moemoe.core.service;

import com.moemoe.client.aws.AwsS3Client;
import com.moemoe.core.request.RegisterProductServiceRequest;
import com.moemoe.core.response.GetProductsResponse;
import com.moemoe.core.response.IdResponse;
import com.moemoe.core.security.MoeUser;
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
            List<ProductEntity> productEntities = List.of(
                    getProductEntity(new ObjectId().toHexString(), "title1", List.of("tag1"), "detail1", 100L, LocalDateTime.now(), LocalDateTime.now()),
                    getProductEntity(new ObjectId().toHexString(), "title2", List.of("tag1", " tag2"), "detail2", 200L, LocalDateTime.now(), LocalDateTime.now()),
                    getProductEntity(new ObjectId().toHexString(), "title3", List.of("tag3"), "detail3", 300L, LocalDateTime.now(), LocalDateTime.now())
            );

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
            List<ProductEntity> expectedPage = productEntities.subList(0, expectedPageSize);
            assertThat(actual.getContents())
                    .hasSize(expectedPageSize)
                    .extracting(
                            GetProductsResponse.Product::id,
                            GetProductsResponse.Product::title,
                            GetProductsResponse.Product::tagNames,
                            GetProductsResponse.Product::detailedAddress,
                            GetProductsResponse.Product::price,
                            GetProductsResponse.Product::createdAt,
                            GetProductsResponse.Product::updatedAt
                    )
                    .containsExactlyElementsOf(
                            expectedPage.stream()
                                    .map(e -> tuple(
                                            e.getStringId(),
                                            e.getTitle(),
                                            e.getTagNames(),
                                            e.getDetailedAddress(),
                                            e.getPrice(),
                                            e.getCreatedAt(),
                                            e.getUpdatedAt()
                                    ))
                                    .toList()
                    );

            then(productEntityRepository)
                    .should()
                    .findPage(expectedOldNextId, expectedPageSize);
        }

        @Test
        @DisplayName("Should return last page and hasNext=false when repository returns exactly pageSize items and nextId is blank")
        void shouldReturnLastPageAndHasNextFalseWhenExactlyPageSizeAndBlankNextId() {
            // given
            int expectedPageSize = 2;
            List<ProductEntity> productEntities = List.of(
                    getProductEntity(new ObjectId().toHexString(), "title1", List.of("tag1"), "detail1", 100L, LocalDateTime.now(), LocalDateTime.now()),
                    getProductEntity(new ObjectId().toHexString(), "title2", List.of("tag1", " tag2"), "detail2", 200L, LocalDateTime.now(), LocalDateTime.now())
            );

            given(productEntityRepository.findPage("", expectedPageSize))
                    .willReturn(productEntities);

            // when
            GetProductsResponse actual = productService.findAll("", expectedPageSize);

            // then
            assertThat(actual)
                    .extracting(GetProductsResponse::getNextId, GetProductsResponse::isHasNext)
                    .containsExactly("", false);
            List<ProductEntity> expectedPage = productEntities.subList(0, expectedPageSize);
            assertThat(actual.getContents())
                    .hasSize(expectedPageSize)
                    .extracting(
                            GetProductsResponse.Product::id,
                            GetProductsResponse.Product::title,
                            GetProductsResponse.Product::tagNames,
                            GetProductsResponse.Product::detailedAddress,
                            GetProductsResponse.Product::price,
                            GetProductsResponse.Product::createdAt,
                            GetProductsResponse.Product::updatedAt
                    )
                    .containsExactlyElementsOf(
                            expectedPage.stream()
                                    .map(e -> tuple(
                                            e.getStringId(),
                                            e.getTitle(),
                                            e.getTagNames(),
                                            e.getDetailedAddress(),
                                            e.getPrice(),
                                            e.getCreatedAt(),
                                            e.getUpdatedAt()
                                    ))
                                    .toList()
                    );
            then(productEntityRepository)
                    .should()
                    .findPage("", expectedPageSize);
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

            then(productEntityRepository)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Should use default pageSize when requestedPageSize is non-positive")
        void shouldUseDefaultPageSizeWhenRequestedSizeIsNonPositive() {
            // given
            given(productEntityRepository.findPage(any(), anyInt()))
                    .willAnswer(inv -> {
                        int ps = inv.getArgument(1, Integer.class);
                        return buildEntities(ps);
                    });

            // when
            GetProductsResponse actual = productService.findAll("", 0);

            // then
            assertThat(actual.isHasNext())
                    .isFalse();
            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            then(productEntityRepository)
                    .should()
                    .findPage(eq(""), captor.capture());
            Integer normalized = captor.getValue();
            assertThat(normalized)
                    .isNotNull()
                    .isPositive();
            assertThat(actual.getContents())
                    .hasSize(normalized);
        }

        @Test
        @DisplayName("Should cap pageSize to max when requestedPageSize exceeds limit")
        void shouldCapPageSizeWhenRequestedSizeExceedsLimit() {
            // given
            given(productEntityRepository.findPage(any(), anyInt()))
                    .willAnswer(inv -> {
                        int ps = inv.getArgument(1, Integer.class);
                        return buildEntities(ps + 1);
                    });

            int huge = Integer.MAX_VALUE;

            // when
            GetProductsResponse actual = productService.findAll("", huge);

            // then
            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            then(productEntityRepository).should().findPage(eq(""), captor.capture());
            Integer normalized = captor.getValue();

            assertThat(normalized)
                    .isNotNull()
                    .isPositive()
                    .isLessThan(huge);
            assertThat(actual.isHasNext()).isTrue();
            assertThat(actual.getContents()).hasSize(normalized);
        }


        private List<ProductEntity> buildEntities(int count) {
            List<ProductEntity> list = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
            for (int i = 0; i < count; i++) {
                list.add(getProductEntity(new ObjectId().toHexString(),
                        "title" + i,
                        List.of("tag" + i),
                        "detail" + i,
                        100L + i,
                        now,
                        now));
            }
            return list;
        }

        private ProductEntity getProductEntity(String id, String title, List<String> tagNames, String detailedAddress, Long price, LocalDateTime createdAt, LocalDateTime updatedAt) {
            ProductEntity mock = BDDMockito.mock(ProductEntity.class);
            given(mock.getStringId())
                    .willReturn(id);
            given(mock.getTitle())
                    .willReturn(title);
            given(mock.getTagNames())
                    .willReturn(tagNames);
            given(mock.getDetailedAddress())
                    .willReturn(detailedAddress);
            given(mock.getPrice())
                    .willReturn(price);
            given(mock.getCreatedAt())
                    .willReturn(createdAt);
            given(mock.getUpdatedAt())
                    .willReturn(updatedAt);
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