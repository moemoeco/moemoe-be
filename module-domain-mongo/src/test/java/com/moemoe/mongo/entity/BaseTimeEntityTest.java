package com.moemoe.mongo.entity;

import com.moemoe.mongo.AbstractMongoDbTest;
import com.moemoe.mongo.repository.TagEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BaseTimeEntityTest extends AbstractMongoDbTest {
    @Autowired
    private TagEntityRepository tagEntityRepository;
    @Test
    @DisplayName("정상 케이스 : 생성, 수정 날짜 정상 동작")
    void validTime() throws InterruptedException {
        // given
        TagEntity tag = TagEntity.of("tag1");
        tagEntityRepository.save(tag);

        // when
        List<TagEntity> all = tagEntityRepository.findAll();
        assertThat(all)
                .hasSize(1)
                .extracting(BaseTimeEntity::getCreatedAt, BaseTimeEntity::getUpdatedAt)
                .isNotNull();

        Thread.sleep(1000);
        tagEntityRepository.decrementProductsCount("tag1");

        // then
        List<TagEntity> actual = tagEntityRepository.findAll();

        TagEntity actualTagEntity = actual.get(0);
        TagEntity expectedTagEntity = all.get(0);
        assertThat(actualTagEntity.getCreatedAt())
                .isEqualTo(expectedTagEntity.getCreatedAt());
        assertThat(actualTagEntity.getUpdatedAt())
                .isNotEqualTo(expectedTagEntity.getUpdatedAt());
    }
}