package com.moemoe.core.service;

import com.moemoe.core.response.GetTagsResponse;
import com.moemoe.mongo.entity.TagEntity;
import com.moemoe.mongo.repository.TagEntityRepository;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {
    @InjectMocks
    private TagService tagService;
    @Mock
    private TagEntityRepository tagEntityRepository;

    @Test
    @DisplayName("성공 케이스 : prefix를 이용해 상위 20개의 태그 조회")
    void findTop20TagsByPrefix() {
        // given
        String prefix = "mongo";
        List<TagEntity> expectedTagEntityList = List.of(
                TagEntity.of("tag1", 10L),
                TagEntity.of("tag2", 6L),
                TagEntity.of("tag4", 5L),
                TagEntity.of("tag3", 4L),
                TagEntity.of("tag5", 4L)
        );
        given(tagEntityRepository.findTop20ByNameStartingWith(prefix, Sort.by(Sort.Order.desc("productsCount"), Sort.Order.asc("name"))))
                .willReturn(expectedTagEntityList);

        // when
        GetTagsResponse actual = tagService.findTop20TagsByPrefix(prefix);

        // then
        assertThat(actual.getContent())
                .hasSize(5)
                .extracting(GetTagsResponse.Tag::getName, GetTagsResponse.Tag::getProductsCount)
                .containsExactly(
                        Tuple.tuple("tag1", 10L),
                        Tuple.tuple("tag2", 6L),
                        Tuple.tuple("tag4", 5L),
                        Tuple.tuple("tag3", 4L),
                        Tuple.tuple("tag5", 4L)
                );
    }

    @Nested
    @DisplayName("Manipulating productsCount of tags")
    class ProductCount {
        @Test
        @DisplayName("increments productsCount when tag already exists")
        void incrementProductsCount_existing() {
            // given
            String tagName = "tag";
            List<String> expectedTagNames = List.of(tagName);
            given(tagEntityRepository.findTagEntityByName(tagName))
                    .willReturn(Optional.of(TagEntity.of(tagName)));

            // when
            tagService.incrementProductsCount(expectedTagNames);

            // then
            then(tagEntityRepository)
                    .should(times(1))
                    .findTagEntityByName(tagName);
            then(tagEntityRepository)
                    .should(times(1))
                    .incrementProductsCount(tagName);
        }

        @Test
        @DisplayName("creates new tag and sets productsCount to 1 when not exists")
        void incrementProductsCount_new() {
            // given
            String tagName = "tag";
            List<String> expectedTagNames = List.of(tagName);
            given(tagEntityRepository.findTagEntityByName(tagName))
                    .willReturn(Optional.empty());

            // when
            tagService.incrementProductsCount(expectedTagNames);

            // then
            then(tagEntityRepository)
                    .should(times(1))
                    .findTagEntityByName(tagName);
            ArgumentCaptor<TagEntity> captor = ArgumentCaptor.forClass(TagEntity.class);
            then(tagEntityRepository)
                    .should(times(1))
                    .save(captor.capture());
            assertThat(captor.getValue())
                    .extracting(TagEntity::getName, TagEntity::getProductsCount)
                    .containsExactly(tagName, 1L);
        }
    }

}