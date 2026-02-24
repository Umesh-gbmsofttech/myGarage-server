package com.gbm.app.dto;

import lombok.Data;

@Data
public class BannerResponse {
    private Long id;
    private Long imageId;
    private String imageUrl;
    private boolean active;
}
