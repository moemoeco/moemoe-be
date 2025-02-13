package com.moemoe.mongo.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Document(collection = "tags")
@CompoundIndex(def = "{'productsCount': -1, 'name': 1}")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Tag {
    @Id
    private String name;
    private long productsCount;
    @CreatedDate
    private LocalDateTime createdDate;

    private Tag(String name, long productsCount) {
        this.name = name.replace(" ", "_");
        this.productsCount = productsCount;
    }

    public static Tag of(String name) {
        return new Tag(name, 0L);
    }

    public static Tag of(String name, long productsCount) {
        return new Tag(name, productsCount);
    }
}
