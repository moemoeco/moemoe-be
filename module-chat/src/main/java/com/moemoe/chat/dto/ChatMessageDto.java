package com.moemoe.chat.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageDto {
    private final String id;
    private final String roomId;
    private final String senderId;
    private final String message;
    private final LocalDateTime createdAt;
    private final int unreadCount;

    @Builder(access = AccessLevel.PRIVATE)
    private ChatMessageDto(String id, String roomId, String senderId, String message, LocalDateTime createdAt, int unreadCount) {
        this.id = id;
        this.roomId = roomId;
        this.senderId = senderId;
        this.message = message;
        this.createdAt = createdAt;
        this.unreadCount = unreadCount;
    }

    public static ChatMessageDto of(String id, String roomId, String senderId, String message, LocalDateTime createdAt, int unreadCount) {
        return ChatMessageDto.builder()
                .id(id)
                .roomId(roomId)
                .senderId(senderId)
                .message(message)
                .createdAt(createdAt)
                .unreadCount(unreadCount)
                .build();
    }
}
