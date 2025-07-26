package com.moemoe.mongo.repository;

import com.moemoe.mongo.AbstractMongoDbTest;
import com.moemoe.mongo.constant.ProductCondition;
import com.moemoe.mongo.entity.ProductEntity;
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
        ProductEntity productEntity = ProductEntity.of(
                new ObjectId(),
                "Valid Product",
                "This is a valid product.",
                ProductEntity.Location.of(37.5665, 126.9780, "Seoul"),
                1000,
                List.of("image1.jpg", "image2.jpg"),
                List.of("tag1", "tag2"),
                ProductCondition.NEW);

        // when
        ProductEntity savedProductEntity = productEntityRepository.save(productEntity);

        // then
        assertThat(savedProductEntity)
                .isNotNull();
        assertThat(savedProductEntity.getId())
                .isNotNull();
        assertThat(savedProductEntity.getSellerId())
                .isEqualTo(productEntity.getSellerId());
        assertThat(savedProductEntity.getTitle())
                .isEqualTo(productEntity.getTitle());
        assertThat(savedProductEntity.getDescription())
                .isEqualTo(productEntity.getDescription());
        assertThat(savedProductEntity.getLocation())
                .isEqualTo(productEntity.getLocation());
        assertThat(savedProductEntity.getPrice())
                .isEqualTo(productEntity.getPrice());
        assertThat(savedProductEntity.getImageKeys())
                .isEqualTo(productEntity.getImageKeys());
        assertThat(savedProductEntity.getTagNames())
                .isEqualTo(productEntity.getTagNames());
        assertThat(savedProductEntity.getCondition())
                .isEqualTo(productEntity.getCondition());
    }
}
