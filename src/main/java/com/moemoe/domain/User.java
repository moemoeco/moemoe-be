package com.moemoe.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "USERS")
public class User {
    @Id
    private String id;
    private String name;

    @Builder
    public User(String name) {
        this.name = name;
    }
}
