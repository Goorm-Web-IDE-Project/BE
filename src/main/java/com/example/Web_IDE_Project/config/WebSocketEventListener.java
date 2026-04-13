package com.example.Web_IDE_Project.config;

import com.example.Web_IDE_Project.controller.ChatController;
import com.example.Web_IDE_Project.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashSet;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes != null) {
            String username = (String) sessionAttributes.get("username");

            if (username != null) {
                log.info("사용자 연결 종료 감지: {}", username);

                ChatController.userList.remove(username);

                ChatMessage chatMessage = ChatMessage.builder()
                        .type(ChatMessage.MessageType.LEAVE)
                        .sender(username)
                        .content(username + "님이 퇴장하셨습니다.")
                        .userList(new HashSet<>(ChatController.userList))
                        .build();

                messagingTemplate.convertAndSend("/sub/chat/room/global", chatMessage);
            }
        }
    }
}