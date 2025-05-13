package com.moemoe.mongo.repository.custom;

import com.moemoe.mongo.AbstractMongoDbTest;
import com.moemoe.mongo.entity.ChatRoomEntity;
import com.moemoe.mongo.repository.custom.impl.ChatRoomEntityFindingRepositoryImpl;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


class ChatRoomEntityFindingRepositoryTest extends AbstractMongoDbTest {
    @Autowired
    private ChatRoomEntityFindingRepositoryImpl chatRoomEntityFindingRepository;


    @Test
    @DisplayName("성공 케이스 : 참가자 아이디로 생성된 채팅룸이 존재하는지 여부")
    void findByParticipantIds() {
        // given
        ObjectId participantId1 = new ObjectId();
        ObjectId participantId2 = new ObjectId();
        ChatRoomEntity chatRoomEntity = ChatRoomEntity.of("", Set.of(participantId1, participantId2));
        mongoTemplate.save(chatRoomEntity);

        // when & then
        assertThat(chatRoomEntityFindingRepository.findByParticipantIds(Set.of(participantId1)))
                .isEmpty();
        assertThat(chatRoomEntityFindingRepository.findByParticipantIds(Set.of(participantId1, participantId2)))
                .isNotEmpty()
                .get()
                .extracting(ChatRoomEntity::getId)
                .isEqualTo(chatRoomEntity.getId());
    }
}