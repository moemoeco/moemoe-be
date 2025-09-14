package com.moemoe.mongo.repository;

import com.moemoe.mongo.AbstractMongoDbTest;
import com.moemoe.mongo.constant.ProductCondition;
import com.moemoe.mongo.entity.ProductEntity;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductEntityRepositoryTest extends AbstractMongoDbTest {
    @Autowired
    private ProductEntityRepository productEntityRepository;

    @Test
    @DisplayName("Should save and find product when valid entity is provided")
    void shouldSaveAndFindProductWhenValidEntityIsProvided() {
        // given
        ProductEntity productEntity = ProductEntity.of(
                new ObjectId(),
                "Valid Product",
                "This is a valid product.",
                ProductEntity.Location.of(37.5665, 126.9780, "Seoul"),
                1000,
                List.of("image1.jpg", "image2.jpg"),
                List.of("tag1", "tag2"),
                ProductCondition.NEW
        );

        // when
        ProductEntity saved = productEntityRepository.save(productEntity);
        ProductEntity actual = productEntityRepository.findById(saved.getId())
                .orElseThrow();

        // then
        assertThat(actual.getId())
                .isNotNull();
        assertThat(actual)
                .usingRecursiveComparison()
                .withEqualsForType(
                        (LocalDateTime a, LocalDateTime b) ->
                                a.truncatedTo(ChronoUnit.MILLIS)
                                        .equals(b.truncatedTo(ChronoUnit.MILLIS)),
                        LocalDateTime.class
                )
                .isEqualTo(productEntity);
    }
}
