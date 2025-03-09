package com.moemoe.mongo.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum UserRole {
    NOT_USER("Not a registered user"),
    USER("Regular user"),
    ADMIN("Administrator");

    private final String description;
}
