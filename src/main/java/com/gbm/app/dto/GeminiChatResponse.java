package com.gbm.app.dto;

public class GeminiChatResponse {
    private String reply;

    public GeminiChatResponse() {
    }

    public GeminiChatResponse(String reply) {
        this.reply = reply;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}
