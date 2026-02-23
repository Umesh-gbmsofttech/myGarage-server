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
    private List<RouteStep> steps;

    @Data
    @AllArgsConstructor
    public static class RoutePoint {
        private double latitude;
        private double longitude;
    }

    @Data
    @AllArgsConstructor
    public static class RouteStep {
        private String instruction;
        private double distanceMeters;
        private double durationSeconds;
        private Integer maneuverType;
    }
}
