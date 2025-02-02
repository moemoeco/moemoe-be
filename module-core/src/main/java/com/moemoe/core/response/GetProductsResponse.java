package com.moemoe.core.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@NoArgsConstructor
public class GetProductsResponse {
    private List<Product> contents;
    private String nextId;
    private boolean hasNext;

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
    @NoArgsConstructor
    public static class Product {
        private String id;
        private String title;
        private List<String> tagIdList;
        private String thumbnailUrl;
        private String detailedAddress;
        private long price;
        private String createAt;

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
