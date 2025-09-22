package com.moemoe.mongo.repository;

import com.moemoe.mongo.AbstractMongoDbTest;
import com.moemoe.mongo.entity.ProductEntity;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductEntityPagingRepositoryTest extends AbstractMongoDbTest {
    @Autowired
    private ProductEntityRepository productEntityRepository;
    private final List<ProductEntity> productEntityList = new ArrayList<>();

    @BeforeEach
    void init() {
        for (int i = 0; i < 10; i++) {
            productEntityList.add(ProductEntity.of(
                    new ObjectId(), String.valueOf(i),
                    null, null, 1L, null, null, null
            ));
        }
        productEntityRepository.saveAll(productEntityList);
    }

    @AfterEach
    void destroy() {
        productEntityRepository.deleteAll();
        productEntityList.clear();
    }

    @Nested
    @DisplayName("findPage 메서드 테스트")
    class FindPageTests {
        // 1) nextId blank → 최신 4건(+1 전략)
        @Test
        @DisplayName("Should return latest products in descending _id order when nextId is blank")
        void shouldReturnLatestInDescOrderWhenNextIdIsBlank() {
            // expected: _id DESC 상위 4건
            List<ProductEntity> expected = productEntityList.stream()
                    .sorted(Comparator.comparing(ProductEntity::getId).reversed())
                    .limit(4)
                    .toList();

            // when
            List<ProductEntity> actual = productEntityRepository.findPage("  ", 3);

            // then
            assertThat(actual)
                    .hasSize(4)
                    .containsExactlyElementsOf(expected);
        }

        // 2) nextId = 인덱스 4의 _id → 그 미만 상위 4건(+1)
        @Test
        @DisplayName("Should return documents with _id less than nextId when nextId is provided")
        void shouldReturnDocsLessThanNextId() {
            ObjectId nextId = productEntityList.get(4).getId();

            // expected: nextId 미만만 추려 _id DESC 상위 4건
            List<ProductEntity> expected = productEntityList.stream()
                    .filter(e -> e.getId().compareTo(nextId) < 0)
                    .sorted(Comparator.comparing(ProductEntity::getId).reversed())
                    .limit(4)
                    .toList();

            // when
            List<ProductEntity> actual = productEntityRepository.findPage(nextId.toHexString(), 3);

            // then
            assertThat(actual)
                    .hasSize(4)
                    .containsExactlyElementsOf(expected);
        }

        // 3) smallest → 빈 리스트
        @Test
        @DisplayName("Should return empty list when nextId is equal to the smallest _id")
        void shouldReturnEmptyWhenNextIdIsSmallest() {
            ObjectId smallest = productEntityList.get(0).getId();

            List<ProductEntity> actual = productEntityRepository.findPage(smallest.toHexString(), 3);

            assertThat(actual).isEmpty();
        }

        // 4) invalid hex → 예외
        @Test
        @DisplayName("Should throw IllegalArgumentException when nextId is invalid hex string")
        void shouldThrowWhenNextIdIsInvalidHex() {
            assertThatThrownBy(() -> productEntityRepository.findPage("not-hex", 3))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        // 5) 정렬 안정성(상위 3건만 비교; 전체는 4건 반환)
        @Test
        @DisplayName("Should keep stable _id descending order even when ObjectIds are close")
        void shouldKeepStableOrderWithCloseObjectIds() {
            List<ProductEntity> expectedTop3 = productEntityList.stream()
                    .sorted(Comparator.comparing(ProductEntity::getId).reversed())
                    .limit(3)
                    .toList();

            List<ProductEntity> actual = productEntityRepository.findPage(null, 3);

            assertThat(actual).hasSize(4);
            assertThat(actual.subList(0, 3))
                    .containsExactlyElementsOf(expectedTop3);
        }

        @Test
        @DisplayName("Should return one item when pageSize is zero (limit = pageSize + 1)")
        void shouldReturnOneItemWhenPageSizeIsZero() {
            // expected: 최신 1건 (ObjectId DESC 첫 번째)
            List<ProductEntity> expected = productEntityList.stream()
                    .sorted(Comparator.comparing(ProductEntity::getId).reversed())
                    .limit(1)
                    .toList();

            // when
            List<ProductEntity> actual = productEntityRepository.findPage(null, 0);

            // then: pageSize=0 → limit(0+1)=1건 반환
            assertThat(actual)
                    .hasSize(1)
                    .containsExactlyElementsOf(expected);
        }
    }
}