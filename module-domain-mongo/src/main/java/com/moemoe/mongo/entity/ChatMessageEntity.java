package com.moemoe.mongo.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageEntity extends BaseTimeEntity {
    @Id
    private ObjectId id;
    private ObjectId chatRoomId;
    private ObjectId userId;
    private String message;

    private ChatMessageEntity(ObjectId chatRoomId, ObjectId userId, String message) {
        this.chatRoomId = chatRoomId;
        this.userId = userId;
        this.message = message;
    }

    public static ChatMessageEntity of(ObjectId chatRoomId, ObjectId userId, String message) {
        return new ChatMessageEntity(chatRoomId, userId, message);
    }
}
