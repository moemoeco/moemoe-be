package com.moemoe.mongo.repository;

import com.moemoe.mongo.AbstractMongoDbTest;
import com.moemoe.mongo.entity.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
        Tag tag = Tag.of("test");
        Tag savedTag = tagEntityRepository.save(tag);
        String name = savedTag.getName();
        assertThat(tagEntityRepository.findAll())
                .hasSize(1)
                .first()
                .extracting(Tag::getProductsCount)
                .isEqualTo(0L);

        // when
        tagEntityRepository.incrementProductsCount(name);

        // then
        Optional<Tag> byId = tagEntityRepository.findById(name);
        if (byId.isEmpty()) {
            fail("테스트 실패");
        } else {
            assertThat(byId)
                    .get()
                    .extracting(Tag::getProductsCount)
                    .isEqualTo(1L);
        }
    }


    @Test
    @DisplayName("성공 케이스 : Products Count 감소")
    void decrementProductsCount() {
        // given
        Tag tag = Tag.of("test", 5L);
        Tag savedTag = tagEntityRepository.save(tag);
        String name = savedTag.getName();
        assertThat(tagEntityRepository.findAll())
                .hasSize(1)
                .first()
                .extracting(Tag::getProductsCount)
                .isEqualTo(5L);

        // when
        tagEntityRepository.decrementProductsCount(name);

        // then
        Optional<Tag> byId = tagEntityRepository.findById(name);
        if (byId.isEmpty()) {
            fail("테스트 실패");
        } else {
            assertThat(byId)
                    .get()
                    .extracting(Tag::getProductsCount)
                    .isEqualTo(4L);
        }
    }
}