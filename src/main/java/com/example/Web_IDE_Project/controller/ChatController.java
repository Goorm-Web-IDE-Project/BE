package com.example.Web_IDE_Project.controller;

import com.example.Web_IDE_Project.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;

    private static final Set<String> userList = Collections.synchronizedSet(new HashSet<>());

    @MessageMapping("/chat/message")
    public void message(ChatMessage message) {
        if (ChatMessage.MessageType.ENTER.equals(message.getType())) {
            userList.add(message.getSender());
            message.setContent(message.getSender() + "님이 입장하셨습니다.");
        }

        else if (ChatMessage.MessageType.LEAVE.equals(message.getType())) {
            userList.remove(message.getSender());
            message.setContent(message.getSender() + "님이 퇴장하셨습니다.");
        }

        message.setUserList(new HashSet<>(userList));

        messagingTemplate.convertAndSend("/sub/chat/room/global", message);
    }
}