package com.gbm.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gbm.app.config.GroqConfig;
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
public class GroqChatService {

    private static final String GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";

    private static final List<String> ADMIN_KEYWORDS = List.of(
            "admin",
            "administrator",
            "admin panel",
            "admin dashboard",
            "super admin",
            "super_admin",
            "admin settings",
            "admin login",
            "admin credentials",
            "manage banners",
            "banner management",
            "manage mechanics",
            "manage users",
            "role management",
            "permissions",
            "promote user",
            "demote user"
    );

    private static final String ADMIN_BLOCK_MESSAGE =
            "I can help with customer features like login, signup, bookings, ratings, feedback, and DIY guidance. " +
            "I can’t assist with admin or management actions. Please contact support if you need admin help.";

    private static final String SYSTEM_PROMPT = String.join("\n",
            "You are the MyGarage in-app assistant. Be concise, friendly, and action-oriented.",
            "Do not provide guidance for admin-only tasks or management features. If asked, politely refuse and redirect.",
            "Focus on helping users with login, signup, booking, ratings, feedback, guidance, and DIY tips.",
            "Never claim you performed actions; provide steps the user can follow in the app.",
            "",
            "App context:",
            "- MyGarage is a mobile app that connects vehicle owners with mechanics.",
            "- Roles: VEHICLE_OWNER (owner) and MECHANIC.",
            "- Main areas: Get Started, Sign In, Sign Up, Home, Book Now, Bookings, Profile, Feedback, Support Chat, DIY.",
            "- Sign up (Owner): Name, Surname, Mobile, Email, Password (optional; default used), optional profile image.",
            "- Sign up (Mechanic): Name, Surname, Mobile, Email, Password, Experience, Speciality, City, Shop Active, optional profile image.",
            "- Login: Email + Password.",
            "- Book Now: choose mechanic, add vehicle details/issue, submit.",
            "- Bookings: owners view and track status; mechanics accept/decline and complete jobs.",
            "- Live tracking appears once a booking is accepted.",
            "- OTP verification may be required for meet/complete steps.",
            "- Ratings/Feedback: rate mechanics and submit platform feedback.",
            "- DIY section provides guidance for common vehicle maintenance tasks."
    );

    private final GroqConfig groqConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GroqChatService(GroqConfig groqConfig, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.groqConfig = groqConfig;
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String chat(String message, String userName, String userRole) {
        if (isAdminQuery(message)) {
            return ADMIN_BLOCK_MESSAGE;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqConfig.getKey());

        String safeName = (userName == null || userName.isBlank()) ? "there" : userName.trim();
        String safeRole = (userRole == null || userRole.isBlank()) ? "unknown" : userRole.trim();
        String userContext = "User context: name=" + safeName + ", role=" + safeRole + ".";

        Map<String, Object> payload = Map.of(
                "model", MODEL,
                "temperature", 0.3,
                "max_tokens", 512,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "system", "content", userContext),
                        Map.of("role", "user", "content", message)
                )
        );

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GROQ_CHAT_URL,
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            return extractReply(response.getBody());
        } catch (HttpStatusCodeException ex) {
            throw mapGroqError(ex);
        } catch (ResourceAccessException ex) {
            throw new UpstreamServiceException(HttpStatus.GATEWAY_TIMEOUT, "Chat request timed out. Please try again.");
        }
    }

    private boolean isAdminQuery(String message) {
        if (message == null) {
            return false;
        }
        String text = message.toLowerCase();
        for (String keyword : ADMIN_KEYWORDS) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String extractReply(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse Groq response.", ex);
        }
    }

    private UpstreamServiceException mapGroqError(HttpStatusCodeException ex) {
        String body = ex.getResponseBodyAsString();
        String message = "Chat request failed.";

        if (body != null && !body.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(body).path("error");
                String parsedMessage = root.path("message").asText("");
                if (!parsedMessage.isBlank()) {
                    message = parsedMessage;
                }
            } catch (Exception ignored) {
                // fallback to default message
            }
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
                    "Chat configuration error: Groq authentication failed."
            );
        }

        return new UpstreamServiceException(HttpStatus.BAD_GATEWAY, message);
    }
}
