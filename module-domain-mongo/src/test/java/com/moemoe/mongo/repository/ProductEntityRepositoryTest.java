package com.moemoe.mongo.repository;

import com.moemoe.mongo.AbstractMongoDbTest;
import com.moemoe.mongo.constant.ProductCondition;
import com.moemoe.mongo.entity.Product;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductEntityRepositoryTest extends AbstractMongoDbTest {
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
                List.of("tag1", "tag2"),
                ProductCondition.NEW);

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
        assertThat(savedProduct.getCondition())
                .isEqualTo(product.getCondition());
    }
}
