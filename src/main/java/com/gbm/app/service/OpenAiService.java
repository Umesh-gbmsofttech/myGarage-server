package com.gbm.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gbm.app.config.ApiKeysConfig;
import com.gbm.app.exception.UpstreamServiceException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private static final String OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";

    private final ApiKeysConfig apiKeysConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiService(ApiKeysConfig apiKeysConfig, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.apiKeysConfig = apiKeysConfig;
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String chat(String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKeysConfig.getOpenAiKey());

        Map<String, Object> payload = Map.of(
                "model", MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a concise and helpful assistant for MyGarage."),
                        Map.of("role", "user", "content", message)
                )
        );

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    OPENAI_CHAT_URL,
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            return extractReply(response.getBody());
        } catch (HttpStatusCodeException ex) {
            throw mapOpenAiError(ex);
        } catch (ResourceAccessException ex) {
            throw new UpstreamServiceException(HttpStatus.GATEWAY_TIMEOUT, "OpenAI request timed out. Please try again.");
        }
    }

    private String extractReply(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse OpenAI response.", ex);
        }
    }

    private UpstreamServiceException mapOpenAiError(HttpStatusCodeException ex) {
        String body = ex.getResponseBodyAsString();
        String code = "";
        String message = "OpenAI request failed.";

        if (body != null && !body.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(body).path("error");
                code = root.path("code").asText("");
                String parsedMessage = root.path("message").asText("");
                if (!parsedMessage.isBlank()) {
                    message = parsedMessage;
                }
            } catch (Exception ignored) {
                // fallback to default message
            }
        }

        if ("billing_not_active".equalsIgnoreCase(code)) {
            return new UpstreamServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chat is unavailable: OpenAI billing is not active for the configured key."
            );
        }

        if (ex.getStatusCode().value() == 429) {
            return new UpstreamServiceException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Chat is temporarily rate-limited. Please retry in a moment."
            );
        }

        if (ex.getStatusCode().value() == 401 || ex.getStatusCode().value() == 403) {
            return new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Chat configuration error: OpenAI authentication failed."
            );
        }

        return new UpstreamServiceException(HttpStatus.BAD_GATEWAY, message);
    }
}
