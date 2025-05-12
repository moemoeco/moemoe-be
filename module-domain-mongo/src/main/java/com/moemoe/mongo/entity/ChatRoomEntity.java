package com.moemoe.mongo.entity;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Getter
@Document(collection = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomEntity extends BaseTimeEntity {
    @Id
    private ObjectId id;
    /**
     * 기본 값은 참가자 명
     */
    private String title;
    private ObjectId displayMessageId;
    private Set<ObjectId> participantIds;

    public ChatRoomEntity(String title, Set<ObjectId> participantIds) {
        this.title = title;
        this.participantIds = participantIds;
    }

    public static ChatRoomEntity of(String title, Set<ObjectId> participantIds) {
        return new ChatRoomEntity(title, participantIds);
    }
}
