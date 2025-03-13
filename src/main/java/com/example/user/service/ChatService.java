package com.example.user.service;

import com.example.user.dto.ChatDto;
import com.example.user.entity.Chat;
import com.example.user.entity.User;
import com.example.user.repository.ChatRepository;
import com.example.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void sendMessage(String rEmail, String sEmail, String content) {
        User rUser = userRepository.findById(rEmail).orElse(null);
        User sUser = userRepository.findById(sEmail).orElse(null);

        if (rUser == null || sUser == null) return;

        Chat chat = new Chat();
        chat.setREmail(rEmail);
        chat.setSEmail(sEmail);
        chat.setContent(content);

        chatRepository.save(chat);

        ChatDto chatDto = new ChatDto();
        chatDto.setREmail(rEmail);
        chatDto.setSEmail(sEmail);
        chatDto.setContent(content);
        chatDto.setId(chat.getId());
        chatDto.setCreated(chat.getCreated());


        messagingTemplate.convertAndSend("/chat/sub/" + rEmail,"알림: 채팅, " + chatDto); // 수신자 이메일에 전송


    }

    public ResponseEntity<?> getChatHistory(String rEmail, String sEmail) {
        User rUser = userRepository.findByEmail(rEmail);
        User sUser = userRepository.findByEmail(sEmail);

        if(rUser == null || sUser == null)  return ResponseEntity.badRequest().body("존재하지 않는 유저");

        List<Chat> chatList = chatRepository.findByREmailAndSEmail(rEmail,sEmail);

        if(chatList.isEmpty() || chatList == null) return ResponseEntity.ok("메시지 내역 없음");

        List<ChatDto> chatDtoList = new ArrayList<>();

        for(Chat chat : chatList) {
            ChatDto chatDto = new ChatDto();
            chatDto.setREmail(chat.getREmail());
            chatDto.setSEmail(chat.getSEmail());
            chatDto.setContent(chat.getContent());
            chatDto.setCreated(chat.getCreated());
            chatDto.setId(chat.getId());

            chatDtoList.add(chatDto);
        }
        return ResponseEntity.ok(chatDtoList);
    }
}
