package com.moemoe.mongo.repository.custom;

import com.moemoe.mongo.AbstractMongoDbTest;
import com.moemoe.mongo.entity.ChatRoomEntity;
import com.moemoe.mongo.repository.custom.impl.ChatRoomEntityUpdateRepositoryImpl;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomEntityUpdateRepositoryTest extends AbstractMongoDbTest {
    @Autowired
    private ChatRoomEntityUpdateRepositoryImpl chatRoomEntityUpdateRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    @DisplayName("성공 케이스 : Display Message ID 업데이트")
    void updateDisplayMessageId() {
        // given
        ChatRoomEntity chatRoomEntity = ChatRoomEntity.of("test", Set.of());
        ChatRoomEntity saved = mongoTemplate.save(chatRoomEntity);
        assertThat(saved.getDisplayMessageId())
                .isNull();

        // when
        ObjectId objectId = new ObjectId();
        chatRoomEntityUpdateRepository.updateDisplayMessageId(saved.getId(), objectId);

        // then
        ChatRoomEntity byId = mongoTemplate.findById(saved.getId(), ChatRoomEntity.class);
        Assertions.assertNotNull(byId);
        assertThat(byId.getDisplayMessageId())
                .isEqualTo(objectId);
    }
}