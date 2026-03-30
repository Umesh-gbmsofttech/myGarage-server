package com.gbm.app.dto;

import lombok.Data;

@Data
public class RouteStatsUpdateRequest {
    private Double distanceKm;
    private Double durationMinutes;
}
