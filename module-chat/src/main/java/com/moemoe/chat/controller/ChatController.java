package com.moemoe.chat.controller;

import com.moemoe.chat.controller.request.SendChatMessageRequest;
import com.moemoe.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.send")
    public void sendChat(SendChatMessageRequest request) {
        chatMessageService.sendMessage(request.getRoomId(), request.getUserId(), request.getContent());
    }
}
