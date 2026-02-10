package com.gbm.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gbm.app.config.ApiKeysConfig;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final ApiKeysConfig apiKeysConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService(ApiKeysConfig apiKeysConfig, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.apiKeysConfig = apiKeysConfig;
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String chat(String message) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
                + apiKeysConfig.getGeminiKey();

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", message);
        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(part));
        payload.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
        return extractReply(response.getBody());
    }

    private String extractReply(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        } catch (Exception ignored) {
            return "";
        }
    }
}
