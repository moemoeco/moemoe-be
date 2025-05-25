package com.moemoe.chat.service;

import com.moemoe.chat.dto.ChatMessageDto;
import com.moemoe.mongo.entity.ChatMessageEntity;
import com.moemoe.mongo.entity.ChatRoomEntity;
import com.moemoe.mongo.repository.ChatMessageEntityRepository;
import com.moemoe.mongo.repository.ChatRoomEntityRepository;
import com.moemoe.redis.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final SimpMessageSendingOperations simpMessageSendingOperations;
    private final ChatRoomEntityRepository chatRoomEntityRepository;
    private final ChatMessageEntityRepository chatMessageEntityRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public void sendMessage(String roomId, String userId, String message) {
        ChatRoomEntity chatRoomEntity = chatRoomEntityRepository.findById(new ObjectId(roomId))
                .orElseThrow();
        Set<String> subscribers = subscriptionRepository.getSubscribers(roomId);
        int unreadCount = chatRoomEntity.getParticipantIds().size() - subscribers.size();

        ChatMessageEntity saved = saveChatMessageEntity(roomId, userId, message, subscribers);
        ObjectId messageId = updateChatRoomDisplayMessageId(saved, chatRoomEntity);
        sendMessage(roomId, userId, message, messageId, saved, unreadCount);

        // todo : 방 목록 썸네일 변경 (/queue/chatRooms)
    }

    private ChatMessageEntity saveChatMessageEntity(String roomId, String userId, String message, Set<String> subscribers) {
        Set<ObjectId> readBy = subscribers.stream()
                .map(ObjectId::new)
                .collect(Collectors.toSet());
        ChatMessageEntity chatMessageEntity = ChatMessageEntity.of(roomId, userId, message, readBy);
        ChatMessageEntity saved = chatMessageEntityRepository.save(chatMessageEntity);
        log.info("Saved message in room {}", roomId);
        return saved;
    }

    private ObjectId updateChatRoomDisplayMessageId(ChatMessageEntity saved, ChatRoomEntity chatRoomEntity) {
        ObjectId messageId = saved.getId();
        chatRoomEntityRepository.updateDisplayMessageId(chatRoomEntity.getId(), messageId);
        return messageId;
    }

    private void sendMessage(String roomId, String userId, String message, ObjectId messageId, ChatMessageEntity chatMessageEntity, int unreadCount) {
        ChatMessageDto chatMessageDto = ChatMessageDto.of(messageId.toHexString(), roomId, userId, message, chatMessageEntity.getCreatedAt(), unreadCount);

        SimpMessageHeaderAccessor ha = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        ha.setContentType(MimeTypeUtils.APPLICATION_JSON);
        simpMessageSendingOperations.convertAndSend("/topic/chat." + roomId, chatMessageDto, ha.getMessageHeaders());
        log.debug("Broadcasted message to /topic/chat.{}", roomId);
    }
}
