package com.moemoe.mongo.repository;

import com.moemoe.mongo.AbstractMongoDbTest;
import com.moemoe.mongo.entity.Product;
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
    private ProductEntityPagingRepository productEntityPagingRepository;
    @Autowired
    private ProductEntityRepository productEntityRepository;
    private List<Product> products = new LinkedList<>();

    @BeforeEach
    void init() {
        for (int i = 0; i < 10; i++) {
            products.add(Product.of(new ObjectId(), String.valueOf(i), null, null, 1L, null, null, null));
        }
        productEntityRepository.saveAll(products);
    }

    @AfterEach
    void destroy() {
        productEntityRepository.deleteAll();
    }

    @Test
    @DisplayName("정상 케이스 : 첫 페이지 조회")
    void findAll() {
        // when
        List<Product> actualProducts = productEntityPagingRepository.findAll("", 3);

        // then
        assertThat(actualProducts)
                .hasSize(4)
                .extracting(Product::getTitle)
                .containsExactly("9", "8", "7", "6");
    }

    @Test
    @DisplayName("정상 케이스 : 다음 페이지 조회")
    void findAllWithNextId() {
        // when
        // 4번 상품
        ObjectId nextId = products.get(4).getId();
        List<Product> actualProducts = productEntityPagingRepository.findAll(nextId.toHexString(), 3);

        // then
        assertThat(actualProducts)
                .hasSize(4)
                .extracting(Product::getTitle)
                .containsExactly("3", "2", "1", "0");
    }

    @Test
    @DisplayName("정상 케이스 : 마지막 페이지 조회")
    void findAllWithLastId() {
        // when
        // 4번 상품
        ObjectId nextId = products.get(1).getId();
        List<Product> actualProducts = productEntityPagingRepository.findAll(nextId.toHexString(), 3);

        // then
        assertThat(actualProducts)
                .hasSize(1)
                .extracting(Product::getTitle)
                .containsExactly("0");
    }

}