package com.moemoe.mongo.repository;

import com.moemoe.mongo.entity.ChatMessageEntity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageEntityRepository extends MongoRepository<ChatMessageEntity, ObjectId> {
}
