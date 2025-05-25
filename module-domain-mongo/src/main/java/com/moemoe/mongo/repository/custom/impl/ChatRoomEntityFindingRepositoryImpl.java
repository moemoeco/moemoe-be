package com.moemoe.mongo.repository.custom.impl;

import com.moemoe.mongo.entity.ChatRoomEntity;
import com.moemoe.mongo.repository.custom.ChatRoomEntityFindingRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ChatRoomEntityFindingRepositoryImpl implements ChatRoomEntityFindingRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<ChatRoomEntity> findByParticipantIds(Set<ObjectId> participantIds) {
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("participantIds").all(participantIds),
                Criteria.where("participantIds").size(participantIds.size())
        );
        Query query = Query.query(criteria);
        return Optional.ofNullable(mongoTemplate.findOne(query, ChatRoomEntity.class));
    }
}
