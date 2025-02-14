package com.moemoe.core.service;

import com.moemoe.core.response.GetTagsResponse;
import com.moemoe.mongo.entity.Tag;
import com.moemoe.mongo.repository.TagEntityRepository;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
        List<Tag> expectedTagList = List.of(
                Tag.of("tag1", 10L),
                Tag.of("tag2", 6L),
                Tag.of("tag4", 5L),
                Tag.of("tag3", 4L),
                Tag.of("tag5", 4L)
        );
        given(tagEntityRepository.findTop20ByNameStartingWith(prefix, Sort.by(Sort.Order.desc("productsCount"), Sort.Order.asc("name"))))
                .willReturn(expectedTagList);

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
}