package com.moemoe.chat.service;

import com.moemoe.mongo.entity.ChatRoomEntity;
import com.moemoe.mongo.entity.UserEntity;
import com.moemoe.mongo.repository.ChatRoomEntityRepository;
import com.moemoe.mongo.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomEntityRepository chatRoomEntityRepository;
    private final UserEntityRepository userEntityRepository;

    @Transactional
    public ObjectId create(Set<ObjectId> participantIds) {
        if (ObjectUtils.isEmpty(participantIds)) {
            throw new IllegalArgumentException("Participant list is empty");
        }
        Optional<ChatRoomEntity> byParticipantIds = chatRoomEntityRepository.findByParticipantIds(participantIds);
        if (byParticipantIds.isPresent()) {
            ChatRoomEntity chatRoomEntity = byParticipantIds.get();
            log.info("A chat room with the requested participants already exists (roomId={})", chatRoomEntity.getId());
            return chatRoomEntity.getId();
        }

        log.info("Starting to create chat room with {} participants", participantIds.size());

        List<UserEntity> users = userEntityRepository.findAllById(participantIds);
        if (users.size() != participantIds.size()) {
            throw new IllegalArgumentException("Some participants do not exist");
        }

        String title = users.stream()
                .map(UserEntity::getName)
                .sorted()
                .collect(Collectors.joining(","));

        try {
            ChatRoomEntity chatRoomEntity = ChatRoomEntity.of(title, participantIds);
            ObjectId chatRoomId = chatRoomEntityRepository.save(chatRoomEntity).getId();
            log.info("Successfully created chat room with ID: {}", chatRoomId);
            return chatRoomId;
        } catch (Exception e) {
            log.error("Failed to create chat room", e);
            throw new IllegalStateException("Failed to create chat room", e);
        }
    }
}