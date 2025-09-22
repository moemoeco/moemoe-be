package com.moemoe.core.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class GetProductsResponse {
    private List<Product> contents;
    private String nextId;
    private boolean hasNext;

    public GetProductsResponse(List<Product> contents, int pageSize) {
        if (contents.size() > pageSize) {
            this.contents = contents.subList(0, pageSize);
            this.nextId = this.contents.getLast().id();
            this.hasNext = true;
        } else {
            this.contents = contents;
            this.nextId = "";
            this.hasNext = false;
        }
    }

    public record Product(
            String id,
            String title,
            List<String> tagNames,
            String thumbnailUrl,
            String detailedAddress,
            long price,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            LocalDateTime createdAt,
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            LocalDateTime updatedAt
    ) {
    }
}
