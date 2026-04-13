package com.example.Web_IDE_Project.config;

import com.example.Web_IDE_Project.controller.ChatController; // ChatController 임포트 확인
import com.example.Web_IDE_Project.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String username = (String) headerAccessor.getSessionAttributes().get("username");

        if (username != null) {
            log.info("사용자 연결 종료: {}", username);

            ChatController.userList.remove(username);

            ChatMessage chatMessage = ChatMessage.builder()
                    .type(ChatMessage.MessageType.LEAVE)
                    .sender(username)
                    .content(username + "님이 퇴장하셨습니다.")
                    .userList(new HashSet<>(ChatController.userList)) // 숫 동기화를 위해 필수!
                    .build();

            messagingTemplate.convertAndSend("/sub/chat/room/global", chatMessage);
        }
    }
}