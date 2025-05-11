package com.moemoe.mongo.repository;

import com.moemoe.mongo.entity.ChatRoomEntity;
import com.moemoe.mongo.repository.custom.ChatRoomEntityUpdateRepository;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatRoomEntityRepository extends MongoRepository<ChatRoomEntity, ObjectId>, ChatRoomEntityUpdateRepository {
}
