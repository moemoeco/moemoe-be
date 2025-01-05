package com.moemoe.core.service;

import com.moemoe.client.aws.AwsS3Client;
import com.moemoe.client.exception.ClientRuntimeException;
import com.moemoe.core.request.RegisterProductRequest;
import com.moemoe.mongo.entity.Product;
import com.moemoe.mongo.repository.ProductEntityRepository;
import com.moemoe.mongo.repository.UserEntityRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
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
    private AwsS3Client awsS3Client;


    @Test
    @DisplayName("정상 케이스 : 상품 등록이 완료된 경우")
    void register() {
        // given
        RegisterProductRequest expectedRequest = new RegisterProductRequest();
        setRequestField(expectedRequest);
        given(userEntityRepository.existsById(expectedRequest.getSellerId())).willReturn(true);

        List<MultipartFile> expectedImageList = getMultipartFiles();
        S3Client s3Client = S3Client.builder().build();
        given(awsS3Client.getS3Client())
                .willReturn(s3Client);
        String expectedImageUrl1 = Path.of(expectedRequest.getSellerId().toHexString(), expectedImageList.get(0).getOriginalFilename()).toString();
        String expectedImageUrl2 = Path.of(expectedRequest.getSellerId().toHexString(), expectedImageList.get(1).getOriginalFilename()).toString();
        given(awsS3Client.upload(s3Client, expectedImageUrl1, expectedImageList.get(0)))
                .willReturn(expectedImageUrl1);
        given(awsS3Client.upload(s3Client, expectedImageUrl2, expectedImageList.get(0)))
                .willReturn(expectedImageUrl2);

        Product sampleProduct = Product.of(expectedRequest.getSellerId(),
                "Sample Product",
                "This is a sample product.",
                Product.Location.of(expectedRequest.getLatitude(), expectedRequest.getLongitude(), expectedRequest.getDetailAddress()),
                1000L,
                List.of(expectedImageUrl1, expectedImageUrl2),
                List.of("tag1", "tag2"));
        ObjectId expectedProductId = new ObjectId();
        ReflectionTestUtils.setField(sampleProduct, "id", expectedProductId);
        given(productEntityRepository.save(Mockito.any(Product.class))).willReturn(sampleProduct);

        // when
        String actualId = productService.register(expectedRequest, expectedImageList);

        // then
        assertThat(actualId)
                .isEqualTo(expectedProductId.toHexString());

        verify(userEntityRepository, times(1)).existsById(expectedRequest.getSellerId());
        verify(awsS3Client, times(1)).getS3Client();
        verify(awsS3Client, times(1)).upload(s3Client, expectedImageUrl1, expectedImageList.get(0));
        verify(awsS3Client, times(1)).upload(s3Client, expectedImageUrl2, expectedImageList.get(1));
    }

    @Test
    @DisplayName("실패 케이스1 : Seller Id가 존재하지 않는 경우")
    void registerWithSellerException() {
        // given
        RegisterProductRequest expectedRequest = new RegisterProductRequest();
        setRequestField(expectedRequest);
        given(userEntityRepository.existsById(expectedRequest.getSellerId())).willReturn(false);
        List<MultipartFile> expectedImageList = getMultipartFiles();

        // when
        assertThatThrownBy(() -> productService.register(expectedRequest, expectedImageList))
                .isInstanceOf(IllegalArgumentException.class);

        // then
        verify(userEntityRepository, times(1)).existsById(expectedRequest.getSellerId());
        verify(awsS3Client, times(0)).getS3Client();
        verify(awsS3Client, times(0)).upload(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("실패 케이스2 : S3 업로드 시 에러가 발생한 경우")
    void registerWithS3Exception() {
        // given
        RegisterProductRequest expectedRequest = new RegisterProductRequest();
        setRequestField(expectedRequest);
        given(userEntityRepository.existsById(expectedRequest.getSellerId())).willReturn(true);

        List<MultipartFile> expectedImageList = getMultipartFiles();
        S3Client s3Client = S3Client.builder().build();
        given(awsS3Client.getS3Client())
                .willReturn(s3Client);
        String expectedImageUrl1 = Path.of(expectedRequest.getSellerId().toHexString(), expectedImageList.get(0).getOriginalFilename()).toString();
        String expectedImageUrl2 = Path.of(expectedRequest.getSellerId().toHexString(), expectedImageList.get(1).getOriginalFilename()).toString();
        given(awsS3Client.upload(s3Client, expectedImageUrl1, expectedImageList.get(0)))
                .willReturn(expectedImageUrl1);
        given(awsS3Client.upload(s3Client, expectedImageUrl2, expectedImageList.get(0)))
                .willThrow(new IllegalArgumentException("S3 Exception"));

        // when
        assertThatThrownBy(() -> productService.register(expectedRequest, expectedImageList))
                .isInstanceOf(ClientRuntimeException.class);

        // then
        verify(userEntityRepository, times(1)).existsById(expectedRequest.getSellerId());
        verify(awsS3Client, times(1)).getS3Client();
        verify(awsS3Client, times(1)).upload(s3Client, expectedImageUrl1, expectedImageList.get(0));
        verify(awsS3Client, times(0)).upload(s3Client, expectedImageUrl2, expectedImageList.get(1));
    }

    @Test
    @DisplayName("실패 케이스3 : save 시 에러가 발생한 경우")
    void registerWithMongoException() {
        // given
        RegisterProductRequest expectedRequest = new RegisterProductRequest();
        setRequestField(expectedRequest);
        given(userEntityRepository.existsById(expectedRequest.getSellerId())).willReturn(true);

        List<MultipartFile> expectedImageList = getMultipartFiles();
        S3Client s3Client = S3Client.builder().build();
        given(awsS3Client.getS3Client())
                .willReturn(s3Client);
        String expectedImageUrl1 = Path.of(expectedRequest.getSellerId().toHexString(), expectedImageList.get(0).getOriginalFilename()).toString();
        String expectedImageUrl2 = Path.of(expectedRequest.getSellerId().toHexString(), expectedImageList.get(1).getOriginalFilename()).toString();
        given(awsS3Client.upload(s3Client, expectedImageUrl1, expectedImageList.get(0)))
                .willReturn(expectedImageUrl1);
        given(awsS3Client.upload(s3Client, expectedImageUrl2, expectedImageList.get(0)))
                .willReturn(expectedImageUrl2);

        Product sampleProduct = Product.of(expectedRequest.getSellerId(),
                "Sample Product",
                "This is a sample product.",
                Product.Location.of(expectedRequest.getLatitude(), expectedRequest.getLongitude(), expectedRequest.getDetailAddress()),
                1000L,
                List.of(expectedImageUrl1, expectedImageUrl2),
                List.of("tag1", "tag2"));
        ObjectId expectedProductId = new ObjectId();
        ReflectionTestUtils.setField(sampleProduct, "id", expectedProductId);
        given(productEntityRepository.save(Mockito.any(Product.class))).willThrow(new IllegalArgumentException("Spring Data Mongo Exception"));

        // when
        assertThatThrownBy(() -> productService.register(expectedRequest, expectedImageList))
                .isInstanceOf(IllegalArgumentException.class);

        // then
        verify(userEntityRepository, times(1)).existsById(expectedRequest.getSellerId());
        verify(awsS3Client, times(1)).getS3Client();
        verify(awsS3Client, times(1)).upload(s3Client, expectedImageUrl1, expectedImageList.get(0));
        verify(awsS3Client, times(1)).upload(s3Client, expectedImageUrl2, expectedImageList.get(1));
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

    private void setRequestField(RegisterProductRequest expectedRequest) {
        ReflectionTestUtils.setField(expectedRequest, "sellerId", "64a9ef6a8ed14e3b1c8eb29a");
        ReflectionTestUtils.setField(expectedRequest, "title", "Sample Product");
        ReflectionTestUtils.setField(expectedRequest, "description", "This is a sample product.");

        // Setting Location object
        RegisterProductRequest.Location location = new RegisterProductRequest.Location();
        ReflectionTestUtils.setField(location, "latitude", 37.7749);
        ReflectionTestUtils.setField(location, "longitude", -122.4194);
        ReflectionTestUtils.setField(location, "detailAddress", "123 Sample St, City, Country");
        ReflectionTestUtils.setField(expectedRequest, "location", location);

        ReflectionTestUtils.setField(expectedRequest, "price", 1000L);
        ReflectionTestUtils.setField(expectedRequest, "tagIdList", List.of("tag1", "tag2"));
    }
}