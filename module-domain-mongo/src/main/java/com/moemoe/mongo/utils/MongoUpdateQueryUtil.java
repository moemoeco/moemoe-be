package com.moemoe.mongo.utils;

import lombok.experimental.UtilityClass;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;

@UtilityClass
public class MongoUpdateQueryUtil {
    public static Update withUpdatedAt(Update update) {
        return update.set("updatedAt", LocalDateTime.now());
    }
}
