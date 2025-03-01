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
public class TagEntity {
    @Id
    private String name;
    private long productsCount;
    @CreatedDate
    private LocalDateTime createdDate;

    private TagEntity(String name, long productsCount) {
        this.name = name.replace(" ", "_");
        this.productsCount = productsCount;
    }

    public static TagEntity of(String name) {
        return new TagEntity(name, 0L);
    }

    public static TagEntity of(String name, long productsCount) {
        return new TagEntity(name, productsCount);
    }
}
