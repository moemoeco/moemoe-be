package com.moemoe.mongo.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Getter
@Document(collection = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageEntity extends BaseTimeEntity {
    @Id
    private ObjectId id;
    private ObjectId chatRoomId;
    private ObjectId userId;
    private String message;
    private Set<ObjectId> readBy;

    private ChatMessageEntity(ObjectId chatRoomId,
                              ObjectId userId,
                              String message,
                              Set<ObjectId> readBy) {
        this.chatRoomId = chatRoomId;
        this.userId = userId;
        this.message = message;
        this.readBy = readBy;
    }

    public static ChatMessageEntity of(String chatRoomId,
                                       String userId,
                                       String message,
                                       Set<ObjectId> readBy) {
        return new ChatMessageEntity(new ObjectId(chatRoomId), new ObjectId(userId), message, readBy);
    }
}
