package com.education.message.websocket;

import com.education.message.dto.MessageDTO;
import com.education.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MessageWebSocketHandler {
    
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/chat")
    public void handleMessage(MessageDTO message) {
        log.info("收到WebSocket消息：{}", message);
        MessageDTO saved = messageService.sendMessage(message);
        
        // 发送给特定用户
        messagingTemplate.convertAndSendToUser(
            String.valueOf(message.getReceiverId()),
            "/queue/messages",
            saved
        );
    }
}

