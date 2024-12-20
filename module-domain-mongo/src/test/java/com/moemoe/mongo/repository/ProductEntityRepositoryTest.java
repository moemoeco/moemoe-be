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
        Product product = Product.of(
                new ObjectId(),
                "Valid Product",
                "This is a valid product.",
                "Seoul",
                1000,
                List.of("image1.jpg", "image2.jpg"),
                List.of("tag1", "tag2")
        );

        // 저장
        Product savedProduct = productEntityRepository.save(product);

        // 검증
        assertThat(savedProduct)
                .isNotNull();
        assertThat(savedProduct.getId())
                .isNotNull();
    }

    @Test
    @DisplayName("실패 케이스: sellerId가 null이면 예외가 발생한다.")
    void createProductWithNullSellerId() {
        Product product = Product.of(
                null,
                "Invalid Product",
                null,
                null,
                1000,
                List.of("image1.jpg", "image2.jpg"),
                List.of("tag1", "tag2")
        );

        // 저장 시도 및 예외 검증
        assertThatThrownBy(() -> productEntityRepository.save(product))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Seller Id must not be null");
    }

    @Test
    @DisplayName("실패 케이스: title이 null이면 예외가 발생한다.")
    void createProductWithNullTitle() {
        Product product = Product.of(
                new ObjectId(),
                null,
                null,
                null,
                1000,
                List.of("image1.jpg", "image2.jpg"),
                List.of("tag1", "tag2")
        );

        // 저장 시도 및 예외 검증
        assertThatThrownBy(() -> productEntityRepository.save(product))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Title must not be empty");
    }

    @Test
    @DisplayName("실패 케이스: price가 0이면 예외가 발생한다.")
    void createProductWithZeroPrice() {
        Product product = Product.of(
                new ObjectId(),
                "Invalid Product",
                null,
                null,
                0,
                List.of("image1.jpg", "image2.jpg"),
                List.of("tag1", "tag2")
        );

        // 저장 시도 및 예외 검증
        assertThatThrownBy(() -> productEntityRepository.save(product))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Price must be greater than 0");
    }

    @Test
    @DisplayName("실패 케이스: imageUrlList가 비어있으면 예외가 발생한다.")
    void createProductWithEmptyImageUrlList() {
        Product product = Product.of(
                new ObjectId(),
                "Invalid Product",
                "This product has no images.",
                "Seoul",
                1000,
                List.of(), // 빈 리스트
                List.of("tag1", "tag2")
        );

        assertThatThrownBy(() -> productEntityRepository.save(product))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Images must include at least 1 item and up to 10 items.");
    }

    @Test
    @DisplayName("실패 케이스: imageUrlList가 11개이면 예외가 발생한다.")
    void createProductWithExcessiveImageUrlList() {
        Product product = Product.of(
                new ObjectId(),
                "Invalid Product",
                null,
                null,
                1000,
                List.of("img1.jpg", "img2.jpg", "img3.jpg", "img4.jpg", "img5.jpg",
                        "img6.jpg", "img7.jpg", "img8.jpg", "img9.jpg", "img10.jpg", "img11.jpg"),
                List.of("tag1", "tag2")
        );

        // 저장 시도 및 예외 검증
        assertThatThrownBy(() -> productEntityRepository.save(product))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Images must include at least 1 item and up to 10 items.");
    }

    @Test
    @DisplayName("실패 케이스: tagIdList가 6개이면 예외가 발생한다.")
    void createProductWithExcessiveTagIdList() {
        Product product = Product.of(
                new ObjectId(),
                "Invalid Product",
                null,
                null,
                1000,
                List.of("image1.jpg", "image2.jpg"),
                List.of("tag1", "tag2", "tag3", "tag4", "tag5", "tag6")
        );

        // 저장 시도 및 예외 검증
        assertThatThrownBy(() -> productEntityRepository.save(product))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Tags can include up to 5 items only.");
    }
}
