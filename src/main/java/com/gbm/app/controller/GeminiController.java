package com.gbm.app.controller;

import com.gbm.app.dto.GeminiChatRequest;
import com.gbm.app.dto.GeminiChatResponse;
import com.gbm.app.service.GeminiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gemini")
public class GeminiController {

    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<GeminiChatResponse> chat(@Valid @RequestBody GeminiChatRequest request) {
        String reply = geminiService.chat(request.getMessage());
        return ResponseEntity.ok(new GeminiChatResponse(reply));
    }
}
