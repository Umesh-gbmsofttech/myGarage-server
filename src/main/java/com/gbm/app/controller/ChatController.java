package com.gbm.app.controller;

import com.gbm.app.dto.ChatRequest;
import com.gbm.app.dto.ChatResponse;
import com.gbm.app.service.GroqChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final GroqChatService groqChatService;

    public ChatController(GroqChatService groqChatService) {
        this.groqChatService = groqChatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String reply = groqChatService.chat(request.getMessage(), request.getUserName(), request.getUserRole());
        return ResponseEntity.ok(new ChatResponse(reply));
    }
}
