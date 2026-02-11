package com.gbm.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DirectionsResponse {

    private List<RoutePoint> route;
    private double distanceMeters;
    private double durationSeconds;
    private long etaSeconds;
    private String etaText;

    @Data
    @AllArgsConstructor
    public static class RoutePoint {
        private double latitude;
        private double longitude;
    }
}
