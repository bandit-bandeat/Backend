package com.example.user.controller;

import com.example.user.dto.ChatDto;
import com.example.user.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // chat/pub/send
    @MessageMapping("/send")
    public void sendMessage(@Payload ChatDto chatDto) {
        System.out.println(chatDto);
        chatService.sendMessage(chatDto.getREmail(), chatDto.getSEmail(), chatDto.getContent());
    }

    @GetMapping("/chat/history")
    public ResponseEntity<?> getChatHistory(String rEmail, String sEmail) {
        return chatService.getChatHistory(rEmail,sEmail);
    }

}
