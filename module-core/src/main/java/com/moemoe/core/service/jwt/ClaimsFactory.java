package com.moemoe.core.service.jwt;

import com.moemoe.mongo.entity.User;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public final class ClaimsFactory {
    public static Map<String, String> getUserClaims(User userEntity) {
        return Map.of(
                "email", userEntity.getEmail(),
                "role", userEntity.getRole().name()
        );
    }
}
