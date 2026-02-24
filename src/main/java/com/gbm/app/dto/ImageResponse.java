package com.gbm.app.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ImageResponse {
    private Long id;
    private String fileName;
    private String contentType;
    private Long size;
    private String referenceType;
    private Long referenceId;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
