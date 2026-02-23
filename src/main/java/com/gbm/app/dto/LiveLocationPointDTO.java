package com.gbm.app.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LiveLocationPointDTO {
    private Long userId;
    private Double latitude;
    private Double longitude;
    private Instant updatedAt;
}
