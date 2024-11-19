package com.moemoe.domain.mongo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRole {
    NOT_USER("Not a registered user"),
    USER("Regular user"),
    ADMIN("Administrator");

    private final String description;
}
