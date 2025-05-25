package com.moemoe.mongo.repository.custom;

import com.moemoe.mongo.entity.ChatRoomEntity;
import org.bson.types.ObjectId;

import java.util.Optional;
import java.util.Set;

public interface ChatRoomEntityFindingRepository {
    Optional<ChatRoomEntity> findByParticipantIds(Set<ObjectId> participantIds);
}
