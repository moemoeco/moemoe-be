package com.moemoe.core.service;

import com.moemoe.client.aws.AwsS3Client;
import com.moemoe.client.exception.ClientRuntimeException;
import com.moemoe.core.request.RegisterProductRequest;
import com.moemoe.core.response.GetProductsResponse;
import com.moemoe.core.response.IdResponse;
import com.moemoe.mongo.constant.ProductCondition;
import com.moemoe.mongo.entity.Product;
import com.moemoe.mongo.entity.Tag;
import com.moemoe.mongo.repository.ProductEntityRepository;
import com.moemoe.mongo.repository.TagEntityRepository;
import com.moemoe.mongo.repository.UserEntityRepository;
import org.assertj.core.groups.Tuple;
import org.bson.types.ObjectId;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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


    @Test
    @DisplayName("정상 케이스 : 상품 등록이 완료된 경우")
    void register() {
        // given
        ObjectId expectedSellerId = new ObjectId("64a9ef6a8ed14e3b1c8eb29a");
        String expectedTitle = "Sample Product";
        String expectedDescription = "This is a sample product.";
        long expectedPrice = 1000L;
        List<String> expectedTagName = List.of("tag1", "tag2");
        ProductCondition expectedProductCondition = ProductCondition.DAMAGED;
        double expectedLatitude = 37.7749;
        double expectedLongitude = -122.4194;
        String expectedDetailedAddress = "123 Sample St, City, Country";

        RegisterProductRequest expectedRequest = getMockRegisterProductRequest(
                expectedSellerId,
                expectedTitle,
                expectedDescription,
                expectedPrice,
                expectedTagName,
                expectedProductCondition,
                expectedLatitude,
                expectedLongitude,
                expectedDetailedAddress);

        // validate seller exists
        given(userEntityRepository.existsById(expectedRequest.getSellerId()))
                .willReturn(true);

        // image upload
        List<MultipartFile> expectedImageList = getMultipartFiles();
        S3Client s3Client = BDDMockito.mock(S3Client.class);
        given(awsS3Client.getS3Client())
                .willReturn(s3Client);
        String expectedImageUrl1 = Path.of(expectedRequest.getSellerId().toHexString(), expectedImageList.get(0).getOriginalFilename()).toString();
        String expectedImageUrl2 = Path.of(expectedRequest.getSellerId().toHexString(), expectedImageList.get(1).getOriginalFilename()).toString();
        given(awsS3Client.upload(s3Client, expectedImageUrl1, expectedImageList.get(0)))
                .willReturn(expectedImageUrl1);
        given(awsS3Client.upload(s3Client, expectedImageUrl2, expectedImageList.get(0)))
                .willReturn(expectedImageUrl2);

        // increment tag
        given(tagEntityRepository.findById("tag1"))
                .willReturn(Optional.empty());
        given(tagEntityRepository.findById("tag2"))
                .willReturn(Optional.of(Tag.of("tag2")));

        // product entity save
        Product mockProduct = BDDMockito.mock(Product.class);
        given(productEntityRepository.save(any()))
                .willReturn(mockProduct);
        ObjectId expectedObjectId = new ObjectId();
        given(mockProduct.getId())
                .willReturn(expectedObjectId);

        // when
        IdResponse response = productService.register(expectedRequest, expectedImageList);

        // then
        assertThat(response.getId())
                .isEqualTo(expectedObjectId.toHexString());

        then(userEntityRepository)
                .should(times(1))
                .existsById(expectedRequest.getSellerId());

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

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        then(productEntityRepository)
                .should(times(1))
                .save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getSellerId())
                .isEqualTo(expectedSellerId);
        assertThat(savedProduct.getTitle())
                .isEqualTo(expectedTitle);
        assertThat(savedProduct.getDescription())
                .isEqualTo(expectedDescription);
        assertThat(savedProduct.getPrice())
                .isEqualTo(expectedPrice);
        assertThat(savedProduct.getTagNameList())
                .containsExactlyInAnyOrderElementsOf(expectedTagName);
        assertThat(savedProduct.getCondition())
                .isEqualTo(expectedProductCondition);
        assertThat(savedProduct.getLocation().getLatitude())
                .isEqualTo(expectedLatitude);
        assertThat(savedProduct.getLocation().getLongitude())
                .isEqualTo(expectedLongitude);
        assertThat(savedProduct.getLocation().getDetailedAddress())
                .isEqualTo(expectedDetailedAddress);
    }

    private RegisterProductRequest getMockRegisterProductRequest(ObjectId expectedSellerId, String expectedTitle, String expectedDescription, long expectedPrice, List<String> expectedTagName, ProductCondition expectedProductCondition, double expectedLatitude, double expectedLongitude, String expectedDetailedAddress) {
        RegisterProductRequest expectedRequest = BDDMockito.mock(RegisterProductRequest.class);
        given(expectedRequest.getSellerId())
                .willReturn(expectedSellerId);
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
    @DisplayName("실패 케이스1 : Seller Id가 존재하지 않는 경우")
    void registerWithSellerException() {
        // given
        RegisterProductRequest expectedRequest = BDDMockito.mock(RegisterProductRequest.class);
        given(expectedRequest.getSellerId())
                .willReturn(new ObjectId());
        given(userEntityRepository.existsById(expectedRequest.getSellerId()))
                .willReturn(false);
        List<MultipartFile> expectedImageList = List.of();

        // when
        assertThatThrownBy(() -> productService.register(expectedRequest, expectedImageList))
                .isInstanceOf(IllegalArgumentException.class);

        // then
        then(userEntityRepository)
                .should(times(1))
                .existsById(expectedRequest.getSellerId());

        then(awsS3Client)
                .should(times(0))
                .getS3Client();
        then(awsS3Client)
                .should(times(0))
                .upload(any(), any(), any());

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

    @Test
    @DisplayName("실패 케이스2 : S3 업로드 시 에러가 발생한 경우")
    void registerWithS3Exception() {
        // given
        RegisterProductRequest expectedRequest = BDDMockito.mock(RegisterProductRequest.class);
        given(expectedRequest.getSellerId())
                .willReturn(new ObjectId());
        given(userEntityRepository.existsById(expectedRequest.getSellerId()))
                .willReturn(true);
        List<MultipartFile> expectedImageList = getMultipartFiles();
        S3Client s3Client = BDDMockito.mock(S3Client.class);
        given(awsS3Client.getS3Client())
                .willReturn(s3Client);
        String expectedImageUrl1 = Path.of(expectedRequest.getSellerId().toHexString(), expectedImageList.get(0).getOriginalFilename()).toString();
        String expectedImageUrl2 = Path.of(expectedRequest.getSellerId().toHexString(), expectedImageList.get(1).getOriginalFilename()).toString();
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
                .existsById(expectedRequest.getSellerId());

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
        List<Product> expectedProductEntityList = List.of(
                getProductEntity("0", "detailedAddress1", "thumbnail1"),
                getProductEntity("1", "detailedAddress2", "thumbnail2"),
                getProductEntity("2", "detailedAddress3", "thumbnail3"));

        ObjectId expectedOldNextObjectId = new ObjectId(expectedOldNextId);
        given(productEntityRepository.existsById(expectedOldNextObjectId)).willReturn(true);
        given(productEntityRepository.findAll(expectedOldNextId, expectedPageSize))
                .willReturn(expectedProductEntityList);

        // when
        GetProductsResponse actual = productService.findAll(expectedOldNextId, expectedPageSize);

        // then
        verify(productEntityRepository, times(1)).existsById(expectedOldNextObjectId);
        verify(productEntityRepository, times(1)).findAll(expectedOldNextId, expectedPageSize);
        assertThat(actual)
                .extracting(GetProductsResponse::getNextId, GetProductsResponse::isHasNext)
                .containsExactly(expectedProductEntityList.get(1).getId().toHexString(), true);
        assertThat(actual.getContents())
                .hasSize(expectedPageSize)
                .extracting(GetProductsResponse.Product::getTitle, GetProductsResponse.Product::getDetailedAddress, GetProductsResponse.Product::getThumbnailUrl)
                .containsExactly(
                        Tuple.tuple(expectedProductEntityList.get(0).getTitle(), expectedProductEntityList.get(0).getDetailedAddress(), expectedProductEntityList.get(0).getThumbnailUrl()),
                        Tuple.tuple(expectedProductEntityList.get(1).getTitle(), expectedProductEntityList.get(1).getDetailedAddress(), expectedProductEntityList.get(1).getThumbnailUrl())
                );
    }

    @Test
    @DisplayName("정상 케이스2 : 다음 페이지가 존재하지 않는 경우")
    void findAllLastPage() {
        // given
        String expectedOldNextId = new ObjectId().toHexString();
        int expectedPageSize = 2;
        List<Product> expectedProductEntityList = List.of(
                getProductEntity("0", "detailedAddress1", "thumbnail1"),
                getProductEntity("1", "detailedAddress2", "thumbnail2"));

        ObjectId expectedOldNextObjectId = new ObjectId(expectedOldNextId);
        given(productEntityRepository.existsById(expectedOldNextObjectId)).willReturn(true);
        given(productEntityRepository.findAll(expectedOldNextId, expectedPageSize))
                .willReturn(expectedProductEntityList);

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
                        Tuple.tuple(expectedProductEntityList.get(0).getTitle(), expectedProductEntityList.get(0).getDetailedAddress(), expectedProductEntityList.get(0).getThumbnailUrl()),
                        Tuple.tuple(expectedProductEntityList.get(1).getTitle(), expectedProductEntityList.get(1).getDetailedAddress(), expectedProductEntityList.get(1).getThumbnailUrl())
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

    private Product getProductEntity(String title, String detailedAddress, String thumbnailUrl) {
        Product product = Product.of(new ObjectId(), title, null, Product.Location.of(0, 0, detailedAddress), 1, List.of(thumbnailUrl, "test1", "test2"), null, null);
        ReflectionTestUtils.setField(product, "id", new ObjectId());
        ReflectionTestUtils.setField(product, "createdDate", LocalDateTime.now());
        return product;
    }
}