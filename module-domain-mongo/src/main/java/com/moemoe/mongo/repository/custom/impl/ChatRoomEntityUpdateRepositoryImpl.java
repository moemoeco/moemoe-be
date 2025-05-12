package com.moemoe.mongo.repository.custom.impl;

import com.moemoe.mongo.entity.ChatRoomEntity;
import com.moemoe.mongo.repository.custom.ChatRoomEntityUpdateRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import static com.moemoe.mongo.utils.MongoUpdateQueryUtil.withUpdatedAt;

@Repository
@RequiredArgsConstructor
public class ChatRoomEntityUpdateRepositoryImpl implements ChatRoomEntityUpdateRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public void updateDisplayMessageId(ObjectId id, ObjectId messageId) {
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update()
                .set("displayMessageId", messageId);
        mongoTemplate.updateFirst(query, withUpdatedAt(update), ChatRoomEntity.class);
    }
}
