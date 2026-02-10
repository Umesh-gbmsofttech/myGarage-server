package com.gbm.app.dto;

import jakarta.validation.constraints.NotBlank;

public class GeminiChatRequest {
    @NotBlank
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
