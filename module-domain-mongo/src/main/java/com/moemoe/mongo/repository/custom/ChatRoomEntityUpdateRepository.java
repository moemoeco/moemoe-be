package com.moemoe.mongo.repository.custom;

import org.bson.types.ObjectId;

public interface ChatRoomEntityUpdateRepository {
    void updateDisplayMessageId(ObjectId id, ObjectId messageId);
}
