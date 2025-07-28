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
import org.assertj.core.groups.Tuple;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    @DisplayName("정상 케이스 : 다음 페이지가 존재하는 경우")
    void findAll() {
        // given
        String expectedOldNextId = new ObjectId().toHexString();
        int expectedPageSize = 2;
        String thumbnailUrl1 = "thumbnail1";
        String thumbnailUrl2 = "thumbnail2";
        String thumbnailUrl3 = "thumbnail3";
        List<ProductEntity> expectedProductEntityEntityList = List.of(
                getProductEntity("0", "detailedAddress1", thumbnailUrl1),
                getProductEntity("1", "detailedAddress2", thumbnailUrl2),
                getProductEntity("2", "detailedAddress3", thumbnailUrl3));

        ObjectId expectedOldNextObjectId = new ObjectId(expectedOldNextId);
        given(productEntityRepository.existsById(expectedOldNextObjectId))
                .willReturn(true);
        given(productEntityRepository.findAll(expectedOldNextId, expectedPageSize))
                .willReturn(expectedProductEntityEntityList);

        given(awsS3Client.getPreSignedUrl(thumbnailUrl1))
                .willReturn(thumbnailUrl1);
        given(awsS3Client.getPreSignedUrl(thumbnailUrl2))
                .willReturn(thumbnailUrl2);
        given(awsS3Client.getPreSignedUrl(thumbnailUrl3))
                .willReturn(thumbnailUrl3);

        // when
        GetProductsResponse actual = productService.findAll(expectedOldNextId, expectedPageSize);

        // then
        verify(productEntityRepository, times(1))
                .existsById(expectedOldNextObjectId);
        verify(productEntityRepository, times(1))
                .findAll(expectedOldNextId, expectedPageSize);
        assertThat(actual)
                .extracting(GetProductsResponse::getNextId, GetProductsResponse::isHasNext)
                .containsExactly(expectedProductEntityEntityList.get(1).getId().toHexString(), true);
        assertThat(actual.getContents())
                .hasSize(expectedPageSize)
                .extracting(GetProductsResponse.Product::getTitle, GetProductsResponse.Product::getDetailedAddress, GetProductsResponse.Product::getThumbnailUrl)
                .containsExactly(
                        Tuple.tuple(expectedProductEntityEntityList.get(0).getTitle(), expectedProductEntityEntityList.get(0).getDetailedAddress(), expectedProductEntityEntityList.get(0).getThumbnailUrl()),
                        Tuple.tuple(expectedProductEntityEntityList.get(1).getTitle(), expectedProductEntityEntityList.get(1).getDetailedAddress(), expectedProductEntityEntityList.get(1).getThumbnailUrl())
                );
    }

    @Test
    @DisplayName("정상 케이스2 : 다음 페이지가 존재하지 않는 경우")
    void findAllLastPage() {
        // given
        String expectedOldNextId = new ObjectId().toHexString();
        int expectedPageSize = 2;
        String thumbnailUrl1 = "thumbnail1";
        String thumbnailUrl2 = "thumbnail2";
        List<ProductEntity> expectedProductEntityEntityList = List.of(
                getProductEntity("0", "detailedAddress1", thumbnailUrl1),
                getProductEntity("1", "detailedAddress2", thumbnailUrl2));

        ObjectId expectedOldNextObjectId = new ObjectId(expectedOldNextId);
        given(productEntityRepository.existsById(expectedOldNextObjectId))
                .willReturn(true);
        given(productEntityRepository.findAll(expectedOldNextId, expectedPageSize))
                .willReturn(expectedProductEntityEntityList);


        given(awsS3Client.getPreSignedUrl(thumbnailUrl1))
                .willReturn(thumbnailUrl1);
        given(awsS3Client.getPreSignedUrl(thumbnailUrl2))
                .willReturn(thumbnailUrl2);

        // when
        GetProductsResponse actual = productService.findAll(expectedOldNextId, expectedPageSize);

        // then
        verify(productEntityRepository, times(1)).existsById(expectedOldNextObjectId);
        verify(productEntityRepository, times(1)).findAll(expectedOldNextId, expectedPageSize);
        assertThat(actual)
                .extracting(GetProductsResponse::getNextId, GetProductsResponse::isHasNext)
                .containsExactly("", false);
        assertThat(actual.getContents())
                .hasSize(expectedPageSize)
                .extracting(GetProductsResponse.Product::getTitle, GetProductsResponse.Product::getDetailedAddress, GetProductsResponse.Product::getThumbnailUrl)
                .containsExactly(
                        Tuple.tuple(expectedProductEntityEntityList.get(0).getTitle(), expectedProductEntityEntityList.get(0).getDetailedAddress(), expectedProductEntityEntityList.get(0).getThumbnailUrl()),
                        Tuple.tuple(expectedProductEntityEntityList.get(1).getTitle(), expectedProductEntityEntityList.get(1).getDetailedAddress(), expectedProductEntityEntityList.get(1).getThumbnailUrl())
                );
    }

    @Test
    @DisplayName("실패 케이스 : ID를 가진 데이터가 존재하지 않는 경우")
    void findAllInvalidOldId() {
        // given
        String expectedOldNextId = new ObjectId().toHexString();
        int expectedPageSize = 2;
        ObjectId expectedOldNextObjectId = new ObjectId(expectedOldNextId);
        given(productEntityRepository.existsById(expectedOldNextObjectId))
                .willReturn(false);

        // when
        assertThatThrownBy(() -> productService.findAll(expectedOldNextId, expectedPageSize))
                .isInstanceOf(IllegalArgumentException.class);

        // then
        verify(productEntityRepository, times(1))
                .existsById(expectedOldNextObjectId);
        verify(productEntityRepository, times(0))
                .findAll(expectedOldNextId, expectedPageSize);
    }

    private ProductEntity getProductEntity(String title, String detailedAddress, String thumbnailUrl) {
        ProductEntity productEntity = ProductEntity.of(new ObjectId(), title, null, ProductEntity.Location.of(0, 0, detailedAddress), 1, List.of(thumbnailUrl, "test1", "test2"), null, null);
        ReflectionTestUtils.setField(productEntity, "id", new ObjectId());
        ReflectionTestUtils.setField(productEntity, "createdAt", LocalDateTime.now());
        return productEntity;
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