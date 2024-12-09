package com.moemoe.mongo.repository;

import com.moemoe.mongo.config.MongoConfig;
import com.moemoe.mongo.config.MongoTestConfig;
import com.moemoe.mongo.entity.Product;
import jakarta.validation.ConstraintViolationException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataMongoTest
@ContextConfiguration(classes = {MongoConfig.class, MongoTestConfig.class})
class ProductEntityRepositoryTest {
    @Autowired
    private ProductEntityRepository productEntityRepository;

    @Test
    @DisplayName("정상 케이스: 유효한 데이터를 저장할 수 있다.")
    void createValidProduct() {
        // 정상 데이터
        Product product = Product.builder()
                .sellerId(new ObjectId())
                .title("Valid Product")
                .description("This is a valid product.")
                .price(1000)
                .location("Seoul")
                .imageUrlList(List.of("image1.jpg", "image2.jpg"))
                .tagIdList(List.of("tag1", "tag2"))
                .build();

        // 저장
        Product savedProduct = productEntityRepository.save(product);

        // 검증
        assertThat(savedProduct)
                .isNotNull();
        assertThat(savedProduct.getId())
                .isNotBlank();
    }

    @Test
    @DisplayName("실패 케이스: sellerId가 null이면 예외가 발생한다.")
    void createProductWithNullSellerId() {
        Product product = Product.builder()
                .sellerId(null)
                .title("Invalid Product")
                .price(1000)
                .imageUrlList(List.of("image1.jpg", "image2.jpg"))
                .tagIdList(List.of("tag1", "tag2"))
                .build();

        // 저장 시도 및 예외 검증
        assertThatThrownBy(() -> productEntityRepository.save(product))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Seller Id must not be null");
    }

    @Test
    @DisplayName("실패 케이스: title이 null이면 예외가 발생한다.")
    void createProductWithNullTitle() {
        Product product = Product.builder()
                .sellerId(new ObjectId())
                .title(null)
                .price(1000)
                .imageUrlList(List.of("image1.jpg", "image2.jpg"))
                .tagIdList(List.of("tag1", "tag2"))
                .build();

        // 저장 시도 및 예외 검증
        assertThatThrownBy(() -> productEntityRepository.save(product))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Title must not be empty");
    }

    @Test
    @DisplayName("실패 케이스: price가 0이면 예외가 발생한다.")
    void createProductWithZeroPrice() {
        Product product = Product.builder()
                .sellerId(new ObjectId())
                .title("Invalid Product")
                .price(0)
                .imageUrlList(List.of("image1.jpg", "image2.jpg"))
                .tagIdList(List.of("tag1", "tag2"))
                .build();

        // 저장 시도 및 예외 검증
        assertThatThrownBy(() -> productEntityRepository.save(product))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Price must be greater than 0");
    }

    @Test
    @DisplayName("실패 케이스: imageUrlList가 11개이면 예외가 발생한다.")
    void createProductWithExcessiveImageUrlList() {
        Product product = Product.builder()
                .sellerId(new ObjectId())
                .title("Invalid Product")
                .price(1000)
                .imageUrlList(List.of("img1.jpg", "img2.jpg", "img3.jpg", "img4.jpg", "img5.jpg",
                        "img6.jpg", "img7.jpg", "img8.jpg", "img9.jpg", "img10.jpg", "img11.jpg"))
                .tagIdList(List.of("tag1", "tag2"))
                .build();

        // 저장 시도 및 예외 검증
        assertThatThrownBy(() -> productEntityRepository.save(product))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Images can include up to 10 items only.");
    }

    @Test
    @DisplayName("실패 케이스: tagIdList가 11개이면 예외가 발생한다.")
    void createProductWithExcessiveTagIdList() {
        Product product = Product.builder()
                .sellerId(new ObjectId())
                .title("Invalid Product")
                .price(1000)
                .imageUrlList(List.of("image1.jpg", "image2.jpg"))
                .tagIdList(List.of("tag1", "tag2", "tag3", "tag4", "tag5",
                        "tag6", "tag7", "tag8", "tag9", "tag10", "tag11"))
                .build();

        // 저장 시도 및 예외 검증
        assertThatThrownBy(() -> productEntityRepository.save(product))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Tags can include up to 10 items only.");
    }
}
