package com.gbm.app.dto;

import java.time.Instant;

import com.gbm.app.entity.NotificationType;

import lombok.Data;

@Data
public class NotificationResponse {
    private Long id;
    private String title;
    private String body;
    private NotificationType type;
    private String dataJson;
    private boolean read;
    private Instant createdAt;
}
