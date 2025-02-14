package com.moemoe.core.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GetTagsResponse {
    private List<Tag> content;

    public static GetTagsResponse of(List<Tag> content) {
        return new GetTagsResponse(content);
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Tag {
        private String name;
        private Long productsCount;

        public static Tag of(String name, Long productsCount) {
            return new Tag(name, productsCount);
        }
    }
}
