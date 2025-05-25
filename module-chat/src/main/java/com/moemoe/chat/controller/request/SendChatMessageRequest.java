package com.moemoe.chat.controller.request;

import lombok.Getter;

@Getter
public class SendChatMessageRequest {
    private String roomId;
    private String userId;
    private String content;
}
