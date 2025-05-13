package com.moemoe.chat.service;

import com.moemoe.mongo.entity.ChatRoomEntity;
import com.moemoe.mongo.entity.UserEntity;
import com.moemoe.mongo.repository.ChatRoomEntityRepository;
import com.moemoe.mongo.repository.UserEntityRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {
    @InjectMocks
    private ChatRoomService chatRoomService;
    @Mock
    private UserEntityRepository userEntityRepository;
    @Mock
    private ChatRoomEntityRepository chatRoomEntityRepository;

    @Test
    @DisplayName("성공 케이스 : 참여자가 모두 존재하는 경우 채팅방 생성")
    void shouldCreateChatRoomSuccessfullyIfParticipantsExist() {
        // given
        ObjectId participantId1 = new ObjectId();
        ObjectId participantId2 = new ObjectId();
        Set<ObjectId> participantIds = Set.of(participantId1, participantId2);

        given(chatRoomEntityRepository.findByParticipantIds(participantIds))
                .willReturn(Optional.empty());

        UserEntity user1 = mock(UserEntity.class);
        UserEntity user2 = mock(UserEntity.class);
        given(user1.getName())
                .willReturn("Alice");
        given(user2.getName())
                .willReturn("Bob");

        given(userEntityRepository.findAllById(participantIds))
                .willReturn(List.of(user1, user2));

        ObjectId savedChatRoomId = new ObjectId();
        ChatRoomEntity savedChatRoom = ChatRoomEntity.of("Alice,Bob", participantIds);
        ReflectionTestUtils.setField(savedChatRoom, "id", savedChatRoomId);
        given(chatRoomEntityRepository.save(any(ChatRoomEntity.class)))
                .willReturn(savedChatRoom);

        // when
        ObjectId result = chatRoomService.create(participantIds);

        // then
        assertNotNull(result);
        assertEquals(savedChatRoomId, result);
        then(userEntityRepository)
                .should(times(1))
                .findAllById(participantIds);
        then(chatRoomEntityRepository)
                .should(times(1))
                .save(any(ChatRoomEntity.class));
    }

    @Test
    @DisplayName("성공 케이스 : 이미 참여자가 모두 존재하는 채팅방이 존재하는 경우")
    void shouldExistChatRoom(){
        // given
        ObjectId participantId1 = new ObjectId();
        ObjectId participantId2 = new ObjectId();
        Set<ObjectId> participantIds = Set.of(participantId1, participantId2);

        ObjectId chatRoomId = new ObjectId();
        ChatRoomEntity chatRoomEntity = mock(ChatRoomEntity.class);
        given(chatRoomEntity.getId())
                .willReturn(chatRoomId);
        given(chatRoomEntityRepository.findByParticipantIds(participantIds))
                .willReturn(Optional.of(chatRoomEntity));

        // when
        ObjectId result = chatRoomService.create(participantIds);

        // then
        assertNotNull(result);
        assertEquals(chatRoomId, result);
    }

    @Test
    @DisplayName("실패 케이스 : 참여자 목록이 비어있는 경우 예외 발생")
    void shouldThrowExceptionIfParticipantListIsEmpty() {
        // given
        Set<ObjectId> emptyParticipants = Collections.emptySet();

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            chatRoomService.create(emptyParticipants);
        });

        assertEquals("Participant list is empty", exception.getMessage());
        then(userEntityRepository)
                .shouldHaveNoInteractions();
        then(chatRoomEntityRepository)
                .shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("실패 케이스 : 일부 참여자가 존재하지 않는 경우 예외 발생")
    void shouldThrowExceptionIfSomeParticipantsDoNotExist() {
        // given
        Set<ObjectId> participantIds = Set.of(new ObjectId(), new ObjectId());
        given(chatRoomEntityRepository.findByParticipantIds(participantIds))
                .willReturn(Optional.empty());
        UserEntity user = mock(UserEntity.class);
        given(userEntityRepository.findAllById(participantIds))
                .willReturn(List.of(user));

        // when & then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            chatRoomService.create(participantIds);
        });

        assertEquals("Some participants do not exist", exception.getMessage());
        then(userEntityRepository)
                .should(times(1))
                .findAllById(participantIds);
        then(chatRoomEntityRepository)
                .should(times(1))
                .findByParticipantIds(participantIds);
    }

    @Test
    @DisplayName("실패 케이스 : 채팅방 저장 실패 시 예외 발생")
    void shouldThrowExceptionIfChatRoomSavingFails() {
        // given
        Set<ObjectId> participantIds = Set.of(new ObjectId());
        given(chatRoomEntityRepository.findByParticipantIds(participantIds))
                .willReturn(Optional.empty());
        UserEntity user = mock(UserEntity.class);
        given(user.getName())
                .willReturn("Alice");
        given(userEntityRepository.findAllById(participantIds))
                .willReturn(List.of(user));
        given(chatRoomEntityRepository.save(any(ChatRoomEntity.class)))
                .willThrow(new RuntimeException("Database error"));

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            chatRoomService.create(participantIds);
        });

        assertTrue(exception.getMessage()
                .contains("Failed to create chat room"));
        then(userEntityRepository)
                .should(times(1))
                .findAllById(participantIds);
        then(chatRoomEntityRepository)
                .should(times(1))
                .save(any(ChatRoomEntity.class));
    }
}