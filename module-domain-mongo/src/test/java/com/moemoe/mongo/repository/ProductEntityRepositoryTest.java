package com.moemoe.mongo.repository;

import com.moemoe.mongo.config.MongoConfig;
import com.moemoe.mongo.config.MongoTestConfig;
import com.moemoe.mongo.entity.Product;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@ContextConfiguration(classes = {MongoConfig.class, MongoTestConfig.class})
class ProductEntityRepositoryTest {
    @Autowired
    private ProductEntityRepository productEntityRepository;

    @Test
    @DisplayName("정상 케이스: 유효한 데이터를 저장할 수 있다.")
    void createValidProduct() {
        // given
        Product product = Product.of(
                new ObjectId(),
                "Valid Product",
                "This is a valid product.",
                Product.Location.of(37.5665, 126.9780, "Seoul"),
                1000,
                List.of("image1.jpg", "image2.jpg"),
                List.of("tag1", "tag2")
        );

        // when
        Product savedProduct = productEntityRepository.save(product);

        // then
        assertThat(savedProduct)
                .isNotNull();
        assertThat(savedProduct.getId())
                .isNotNull();
        assertThat(savedProduct.getSellerId())
                .isEqualTo(product.getSellerId());
        assertThat(savedProduct.getTitle())
                .isEqualTo(product.getTitle());
        assertThat(savedProduct.getDescription())
                .isEqualTo(product.getDescription());
        assertThat(savedProduct.getLocation())
                .isEqualTo(product.getLocation());
        assertThat(savedProduct.getPrice())
                .isEqualTo(product.getPrice());
        assertThat(savedProduct.getImageUrlList())
                .isEqualTo(product.getImageUrlList());
        assertThat(savedProduct.getTagIdList())
                .isEqualTo(product.getTagIdList());
    }
}
