package com.gbm.app.controller;

import com.gbm.app.dto.ChatRequest;
import com.gbm.app.dto.ChatResponse;
import com.gbm.app.service.OpenAiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final OpenAiService openAiService;

    public ChatController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String reply = openAiService.chat(request.getMessage());
        return ResponseEntity.ok(new ChatResponse(reply));
    }
}
