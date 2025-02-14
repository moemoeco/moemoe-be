package com.moemoe.api.controller;

import com.moemoe.api.AbstractControllerTest;
import com.moemoe.api.config.handler.ErrorResponseBody;
import com.moemoe.core.response.GetTagsResponse;
import com.moemoe.core.service.TagService;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TagController.class)
class TagControllerTest extends AbstractControllerTest {
    @MockBean
    private TagService tagService;

    @Test
    @DisplayName("성공 케이스 : Tag 조회")
    void getTags() {
        // given
        String prefix = "tag";
        List<GetTagsResponse.Tag> tags = List.of(
                GetTagsResponse.Tag.of("tag1", 10L),
                GetTagsResponse.Tag.of("tag2", 5L),
                GetTagsResponse.Tag.of("tag3", 3L)
        );
        GetTagsResponse response = GetTagsResponse.of(tags);
        given(tagService.findTop20TagsByPrefix(prefix))
                .willReturn(response);

        // when
        MockHttpServletRequestBuilder builder = get("/tags")
                .param("prefix", prefix);
        MvcResult invoke = invoke(builder, status().isOk(), true);
        GetTagsResponse actual = convertResponseToClass(invoke, GetTagsResponse.class);

        // then
        assertThat(actual.getContent())
                .hasSize(3)
                .extracting(GetTagsResponse.Tag::getName, GetTagsResponse.Tag::getProductsCount)
                .containsExactly(
                        Tuple.tuple("tag1", 10L),
                        Tuple.tuple("tag2", 5L),
                        Tuple.tuple("tag3", 3L)
                );
    }

    @Test
    @DisplayName("실패 케이스 : Tag 조회 시 Prefix가 빈 문자열인 경우")
    void getTagsWithoutPrefix() {
        // given

        // when
        MockHttpServletRequestBuilder builder = get("/tags")
                .param("prefix", "");
        MvcResult invoke = invoke(builder, status().isBadRequest(), true);
        ErrorResponseBody actual = convertResponseToClass(invoke, ErrorResponseBody.class);

        // then
        assertThat(actual)
                .extracting(ErrorResponseBody::getType)
                .isEqualTo(HandlerMethodValidationException.class.getSimpleName());
    }
}