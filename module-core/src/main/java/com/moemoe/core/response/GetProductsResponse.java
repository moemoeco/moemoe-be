package com.moemoe.core.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
public class GetProductsResponse {
    private final List<Product> contents;
    private final String nextId;
    private final boolean hasNext;

    public GetProductsResponse(List<Product> contents, int pageSize) {
        if (contents.size() > pageSize) {
            this.contents = contents.subList(0, contents.size() - 1);
            this.nextId = this.contents.getLast().getId();
            this.hasNext = true;
        } else {
            this.contents = contents;
            this.nextId = "";
            this.hasNext = false;
        }
    }

    @Getter
    public static class Product {
        private final String id;
        private final String title;
        private final List<String> tagIdList;
        private final String thumbnailUrl;
        private final String detailedAddress;
        private final long price;
        private final String createAt;

        @Builder
        public Product(String id, String title, List<String> tagIdList, String thumbnailUrl, String detailedAddress, LocalDateTime createAt, long price) {
            this.id = id;
            this.title = title;
            this.tagIdList = tagIdList;
            this.thumbnailUrl = thumbnailUrl;
            this.detailedAddress = detailedAddress;
            this.createAt = createAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
            this.price = price;
        }
    }
}
