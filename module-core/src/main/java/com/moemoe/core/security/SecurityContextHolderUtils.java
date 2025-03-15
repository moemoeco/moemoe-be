package com.moemoe.core.security;

import lombok.experimental.UtilityClass;
import org.bson.types.ObjectId;
import org.springframework.security.core.context.SecurityContextHolder;

@UtilityClass
public class SecurityContextHolderUtils {
    public static ObjectId getUserId() {
        MoeUser moeUser = (MoeUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId = moeUser.getId();
        return new ObjectId(userId);
    }
}
