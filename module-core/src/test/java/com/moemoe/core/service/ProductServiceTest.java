package com.moemoe.core.service;

import com.moemoe.client.aws.AwsS3Client;
import com.moemoe.client.exception.ClientRuntimeException;
import com.moemoe.core.request.RegisterProductRequest;
import com.moemoe.core.response.GetProductsResponse;
import com.moemoe.core.response.IdResponse;
import com.moemoe.mongo.constant.ProductCondition;
import com.moemoe.mongo.entity.ProductEntity;
import com.moemoe.mongo.entity.TagEntity;
import com.moemoe.mongo.repository.ProductEntityRepository;
import com.moemoe.mongo.repository.TagEntityRepository;
import com.moemoe.mongo.repository.UserEntityRepository;
import org.assertj.core.groups.Tuple;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    private AwsS3Client awsS3Client;
    private ObjectId expectedSellerId;

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                "64a9ef6a8ed14e3b1c8eb29a",
                null,
                null
        );
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        expectedSellerId = new ObjectId("64a9ef6a8ed14e3b1c8eb29a");
    }

    @Test
    @DisplayName("정상 케이스 : 상품 등록이 완료된 경우")
    void register() {
        // given
        String expectedTitle = "Sample Product";
        String expectedDescription = "This is a sample product.";
        long expectedPrice = 1000L;
        List<String> expectedTagName = List.of("tag1", "tag2");
        ProductCondition expectedProductCondition = ProductCondition.DAMAGED;
        double expectedLatitude = 37.7749;
        double expectedLongitude = -122.4194;
        String expectedDetailedAddress = "123 Sample St, City, Country";

        RegisterProductRequest expectedRequest = getMockRegisterProductRequest(
                expectedTitle,
                expectedDescription,
                expectedPrice,
                expectedTagName,
                expectedProductCondition,
                expectedLatitude,
                expectedLongitude,
                expectedDetailedAddress);

        // validate seller exists
        given(userEntityRepository.existsById(expectedSellerId))
                .willReturn(true);

        // image upload
        List<MultipartFile> expectedImageList = getMultipartFiles();
        S3Client s3Client = BDDMockito.mock(S3Client.class);
        given(awsS3Client.getS3Client())
                .willReturn(s3Client);
        String expectedImageUrl1 = Path.of(expectedSellerId.toHexString(), expectedImageList.get(0).getOriginalFilename()).toString();
        String expectedImageUrl2 = Path.of(expectedSellerId.toHexString(), expectedImageList.get(1).getOriginalFilename()).toString();
        given(awsS3Client.upload(s3Client, expectedImageUrl1, expectedImageList.get(0)))
                .willReturn(expectedImageUrl1);
        given(awsS3Client.upload(s3Client, expectedImageUrl2, expectedImageList.get(0)))
                .willReturn(expectedImageUrl2);

        // increment tag
        given(tagEntityRepository.findById("tag1"))
                .willReturn(Optional.empty());
        given(tagEntityRepository.findById("tag2"))
                .willReturn(Optional.of(TagEntity.of("tag2")));

        // product entity save
        ProductEntity mockProductEntity = BDDMockito.mock(ProductEntity.class);
        given(productEntityRepository.save(any()))
                .willReturn(mockProductEntity);
        ObjectId expectedObjectId = new ObjectId();
        given(mockProductEntity.getId())
                .willReturn(expectedObjectId);

        // when
        IdResponse response = productService.register(expectedRequest, expectedImageList);

        // then
        assertThat(response.getId())
                .isEqualTo(expectedObjectId.toHexString());

        then(userEntityRepository)
                .should(times(1))
                .existsById(expectedSellerId);

        then(awsS3Client)
                .should(times(1))
                .getS3Client();
        then(awsS3Client)
                .should(times(1))
                .upload(s3Client, expectedImageUrl1, expectedImageList.get(0));
        then(awsS3Client)
                .should(times(1))
                .upload(s3Client, expectedImageUrl2, expectedImageList.get(1));

        then(tagEntityRepository)
                .should(times(1))
                .incrementProductsCount("tag2");
        then(tagEntityRepository)
                .should(times(1))
                .save(argThat(entity -> entity.getName().equals("tag1")));

        ArgumentCaptor<ProductEntity> productCaptor = ArgumentCaptor.forClass(ProductEntity.class);
        then(productEntityRepository)
                .should(times(1))
                .save(productCaptor.capture());
        ProductEntity savedProductEntity = productCaptor.getValue();
        assertThat(savedProductEntity.getSellerId())
                .isEqualTo(expectedSellerId);
        assertThat(savedProductEntity.getTitle())
                .isEqualTo(expectedTitle);
        assertThat(savedProductEntity.getDescription())
                .isEqualTo(expectedDescription);
        assertThat(savedProductEntity.getPrice())
                .isEqualTo(expectedPrice);
        assertThat(savedProductEntity.getTagNameList())
                .containsExactlyInAnyOrderElementsOf(expectedTagName);
        assertThat(savedProductEntity.getCondition())
                .isEqualTo(expectedProductCondition);
        assertThat(savedProductEntity.getLocation().getLatitude())
                .isEqualTo(expectedLatitude);
        assertThat(savedProductEntity.getLocation().getLongitude())
                .isEqualTo(expectedLongitude);
        assertThat(savedProductEntity.getLocation().getDetailedAddress())
                .isEqualTo(expectedDetailedAddress);
    }

    private RegisterProductRequest getMockRegisterProductRequest(String expectedTitle, String expectedDescription, long expectedPrice, List<String> expectedTagName, ProductCondition expectedProductCondition, double expectedLatitude, double expectedLongitude, String expectedDetailedAddress) {
        RegisterProductRequest expectedRequest = BDDMockito.mock(RegisterProductRequest.class);
        given(expectedRequest.getTitle())
                .willReturn(expectedTitle);
        given(expectedRequest.getDescription())
                .willReturn(expectedDescription);
        given(expectedRequest.getPrice())
                .willReturn(expectedPrice);
        given(expectedRequest.getTagNameList())
                .willReturn(expectedTagName);
        given(expectedRequest.getCondition())
                .willReturn(expectedProductCondition);
        given(expectedRequest.getLatitude())
                .willReturn(expectedLatitude);
        given(expectedRequest.getLongitude())
                .willReturn(expectedLongitude);
        given(expectedRequest.getDetailAddress())
                .willReturn(expectedDetailedAddress);
        return expectedRequest;
    }

    @Test
    @DisplayName("실패 케이스2 : S3 업로드 시 에러가 발생한 경우")
    void registerWithS3Exception() {
        // given
        RegisterProductRequest expectedRequest = BDDMockito.mock(RegisterProductRequest.class);
        given(userEntityRepository.existsById(expectedSellerId))
                .willReturn(true);
        List<MultipartFile> expectedImageList = getMultipartFiles();
        S3Client s3Client = BDDMockito.mock(S3Client.class);
        given(awsS3Client.getS3Client())
                .willReturn(s3Client);
        String expectedImageUrl1 = Path.of(expectedSellerId.toHexString(), expectedImageList.get(0).getOriginalFilename()).toString();
        String expectedImageUrl2 = Path.of(expectedSellerId.toHexString(), expectedImageList.get(1).getOriginalFilename()).toString();
        given(awsS3Client.upload(s3Client, expectedImageUrl1, expectedImageList.get(0)))
                .willReturn(expectedImageUrl1);
        given(awsS3Client.upload(s3Client, expectedImageUrl2, expectedImageList.get(1)))
                .willThrow(new IllegalArgumentException("S3 Exception"));

        // when
        assertThatThrownBy(() -> productService.register(expectedRequest, expectedImageList))
                .isInstanceOf(ClientRuntimeException.class);

        // then
        then(userEntityRepository)
                .should(times(1))
                .existsById(expectedSellerId);

        then(awsS3Client)
                .should(times(1))
                .getS3Client();
        then(awsS3Client)
                .should(times(1))
                .upload(s3Client, expectedImageUrl1, expectedImageList.get(0));
        then(awsS3Client)
                .should(times(1))
                .upload(s3Client, expectedImageUrl1, expectedImageList.get(1));

        then(tagEntityRepository)
                .should(times(0))
                .incrementProductsCount(any());
        then(tagEntityRepository)
                .should(times(0))
                .save(any());

        then(productEntityRepository)
                .should(times(0))
                .save(any());
    }

    private List<MultipartFile> getMultipartFiles() {
        MockMultipartFile file1 = new MockMultipartFile(
                "imageList",
                "test1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "imageList",
                "test1.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "".getBytes()
        );
        return List.of(file1, file2);
    }


    @Test
    @DisplayName("정상 케이스 : 다음 페이지가 존재하는 경우")
    void findAll() {
        // given
        String expectedOldNextId = new ObjectId().toHexString();
        int expectedPageSize = 2;
        List<ProductEntity> expectedProductEntityEntityList = List.of(
                getProductEntity("0", "detailedAddress1", "thumbnail1"),
                getProductEntity("1", "detailedAddress2", "thumbnail2"),
                getProductEntity("2", "detailedAddress3", "thumbnail3"));

        ObjectId expectedOldNextObjectId = new ObjectId(expectedOldNextId);
        given(productEntityRepository.existsById(expectedOldNextObjectId)).willReturn(true);
        given(productEntityRepository.findAll(expectedOldNextId, expectedPageSize))
                .willReturn(expectedProductEntityEntityList);

        // when
        GetProductsResponse actual = productService.findAll(expectedOldNextId, expectedPageSize);

        // then
        verify(productEntityRepository, times(1)).existsById(expectedOldNextObjectId);
        verify(productEntityRepository, times(1)).findAll(expectedOldNextId, expectedPageSize);
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
        List<ProductEntity> expectedProductEntityEntityList = List.of(
                getProductEntity("0", "detailedAddress1", "thumbnail1"),
                getProductEntity("1", "detailedAddress2", "thumbnail2"));

        ObjectId expectedOldNextObjectId = new ObjectId(expectedOldNextId);
        given(productEntityRepository.existsById(expectedOldNextObjectId)).willReturn(true);
        given(productEntityRepository.findAll(expectedOldNextId, expectedPageSize))
                .willReturn(expectedProductEntityEntityList);

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
        given(productEntityRepository.existsById(expectedOldNextObjectId)).willReturn(false);

        // when
        assertThatThrownBy(() -> productService.findAll(expectedOldNextId, expectedPageSize))
                .isInstanceOf(IllegalArgumentException.class);

        // then
        verify(productEntityRepository, times(1)).existsById(expectedOldNextObjectId);
        verify(productEntityRepository, times(0)).findAll(expectedOldNextId, expectedPageSize);
    }

    private ProductEntity getProductEntity(String title, String detailedAddress, String thumbnailUrl) {
        ProductEntity productEntity = ProductEntity.of(new ObjectId(), title, null, ProductEntity.Location.of(0, 0, detailedAddress), 1, List.of(thumbnailUrl, "test1", "test2"), null, null);
        ReflectionTestUtils.setField(productEntity, "id", new ObjectId());
        ReflectionTestUtils.setField(productEntity, "createdDate", LocalDateTime.now());
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
        given(tagEntityRepository.findAllById(tagNameList))
                .willReturn(tagEntityList);
        S3Client s3Client = S3Client.builder()
                .build();
        given(awsS3Client.getS3Client())
                .willReturn(s3Client);
        willDoNothing()
                .given(awsS3Client)
                .delete(s3Client, imageUrlList);

        // when
        String productId = productObjectId.toHexString();
        productService.delete(productId);

        // then
        verify(productEntityRepository, times(1))
                .findById(productObjectId);
        verify(tagEntityRepository, times(1))
                .findAllById(tagNameList);
        verify(awsS3Client, times(1))
                .getS3Client();
        verify(awsS3Client, times(1))
                .delete(s3Client, imageUrlList);
        verify(productEntityRepository, times(1))
                .delete(productEntity);
    }
}