package com.moemoe.mongo.repository;

import com.moemoe.mongo.AbstractMongoDbTest;
import com.moemoe.mongo.entity.ProductEntity;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductEntityPagingRepositoryTest extends AbstractMongoDbTest {
    @Autowired
    private ProductEntityRepository productEntityRepository;
    private final List<ProductEntity> productEntities = new LinkedList<>();

    @BeforeEach
    void init() {
        for (int i = 0; i < 10; i++) {
            productEntities.add(ProductEntity.of(new ObjectId(), String.valueOf(i), null, null, 1L, null, null, null));
        }
        productEntityRepository.saveAll(productEntities);
    }

    @AfterEach
    void destroy() {
        productEntityRepository.deleteAll();
    }

    @Test
    @DisplayName("정상 케이스 : 첫 페이지 조회")
    void findAll() {
        // when
        List<ProductEntity> actualProductEntities = productEntityRepository.findAll("", 3);

        // then
        assertThat(actualProductEntities)
                .hasSize(4)
                .extracting(ProductEntity::getTitle)
                .containsExactly("9", "8", "7", "6");
    }

    @Test
    @DisplayName("정상 케이스 : 다음 페이지 조회")
    void findAllWithNextId() {
        // when
        // 4번 상품
        ObjectId nextId = productEntities.get(4).getId();
        List<ProductEntity> actualProductEntities = productEntityRepository.findAll(nextId.toHexString(), 3);

        // then
        assertThat(actualProductEntities)
                .hasSize(4)
                .extracting(ProductEntity::getTitle)
                .containsExactly("3", "2", "1", "0");
    }

    @Test
    @DisplayName("정상 케이스 : 마지막 페이지 조회")
    void findAllWithLastId() {
        // when
        // 4번 상품
        ObjectId nextId = productEntities.get(1).getId();
        List<ProductEntity> actualProductEntities = productEntityRepository.findAll(nextId.toHexString(), 3);

        // then
        assertThat(actualProductEntities)
                .hasSize(1)
                .extracting(ProductEntity::getTitle)
                .containsExactly("0");
    }

}