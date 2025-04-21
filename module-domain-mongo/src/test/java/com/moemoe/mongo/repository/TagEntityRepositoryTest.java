package com.moemoe.mongo.repository;

import com.moemoe.mongo.AbstractMongoDbTest;
import com.moemoe.mongo.entity.TagEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class TagEntityRepositoryTest extends AbstractMongoDbTest {
    @Autowired
    private TagEntityRepository tagEntityRepository;

    @AfterEach
    void destroy() {
        tagEntityRepository.deleteAll();
    }

    @Test
    @DisplayName("성공 케이스 : Products Count 증가")
    void incrementProductsCount() {
        // given
        TagEntity tagEntity = TagEntity.of("test");
        TagEntity savedTagEntity = tagEntityRepository.save(tagEntity);
        String name = savedTagEntity.getName();
        assertThat(tagEntityRepository.findAll())
                .hasSize(1)
                .first()
                .extracting(TagEntity::getProductsCount)
                .isEqualTo(0L);

        // when
        tagEntityRepository.incrementProductsCount(name);

        // then
        Optional<TagEntity> byId = tagEntityRepository.findById(tagEntity.getId());
        if (byId.isEmpty()) {
            fail("테스트 실패");
        } else {
            assertThat(byId)
                    .get()
                    .extracting(TagEntity::getProductsCount)
                    .isEqualTo(1L);
        }
    }


    @Test
    @DisplayName("성공 케이스 : Products Count 감소")
    void decrementProductsCount() {
        // given
        TagEntity tagEntity = TagEntity.of("test", 5L);
        TagEntity savedTagEntity = tagEntityRepository.save(tagEntity);
        String name = savedTagEntity.getName();
        assertThat(tagEntityRepository.findAll())
                .hasSize(1)
                .first()
                .extracting(TagEntity::getProductsCount)
                .isEqualTo(5L);

        // when
        tagEntityRepository.decrementProductsCount(name);

        // then
        Optional<TagEntity> byId = tagEntityRepository.findById(tagEntity.getId());
        if (byId.isEmpty()) {
            fail("테스트 실패");
        } else {
            assertThat(byId)
                    .get()
                    .extracting(TagEntity::getProductsCount)
                    .isEqualTo(4L);
        }
    }

    @Test
    @DisplayName("성공 케이스 : 특정 이름으로 시작하는 상위 20개 태그 조회")
    void findTop20ByNameStartingWith() {
        // given
        String prefix = "mongo";
        for (int i = 0; i < 60; i++) {
            String name;
            if (i % 3 == 0) {
                name = "postgresql";
            } else if (i % 3 == 1) {
                name = "monkey";
            } else {
                name = "mongo";
            }
            tagEntityRepository.save(TagEntity.of(name + i, i));
        }
        assertThat(tagEntityRepository.findAll())
                .hasSize(60);

        // when
        Sort sort = Sort.by(Sort.Order.desc("productsCount"), Sort.Order.asc("name"));
        List<TagEntity> top20ByNameStartingWith = tagEntityRepository.findTop20ByNameStartingWith(prefix, sort);

        // then
        assertThat(top20ByNameStartingWith)
                .hasSize(20)
                .allMatch(entity -> entity.getName().startsWith(prefix));
    }

    @Test
    @DisplayName("성공 케이스 : 이름으로 Tag Entity 조회")
    void findTagEntityByName() {
        // given
        TagEntity tag = TagEntity.of("tag");
        tagEntityRepository.save(tag);

        // when
        Optional<TagEntity> actual = tagEntityRepository.findTagEntityByName("tag");

        // then
        assertThat(actual)
                .isNotEmpty()
                .get()
                .extracting(TagEntity::getName)
                .isEqualTo("tag");
    }

    @Test
    @DisplayName("성공 케이스 : 이름 목록으로 Tag Entity 목록 조회")
    void findAllByNameIn() {
        // given
        TagEntity tag1 = TagEntity.of("tag1");
        TagEntity tag2 = TagEntity.of("tag2");
        TagEntity tag3 = TagEntity.of("tag3");
        tagEntityRepository.saveAll(List.of(tag1, tag2, tag3));

        // when
        List<TagEntity> allByNameIn = tagEntityRepository.findAllByNameIn(List.of("tag1", "tag2", "tag3"));

        // then
        assertThat(allByNameIn)
                .hasSize(3)
                .extracting(TagEntity::getName)
                .containsExactlyInAnyOrder("tag1", "tag2", "tag3");
    }
}