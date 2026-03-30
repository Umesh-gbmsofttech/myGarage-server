package com.gbm.app.dto;

import java.time.Instant;

import com.gbm.app.entity.BookingStatus;

import lombok.Data;

@Data
public class BookingResponse {
    private Long id;
    private Long ownerId;
    private Long mechanicId;
    private Long assignedWorkerId;
    private Boolean assignedWorkerAccepted;
    private BookingStatus status;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleYear;
    private String issueDescription;
    private Double serviceLatitude;
    private Double serviceLongitude;
    private String serviceAddress;
    private String meetOtp;
    private String completeOtp;
    private boolean meetVerified;
    private boolean completeVerified;
    private Instant serviceCompletedAt;
    private String reportDescription;
    private Instant reportCreatedAt;
    private Long reporterUserId;
    private String reporterRole;
    private String ownerProfileImageUrl;
    private String mechanicProfileImageUrl;
    private String assignedWorkerProfileImageUrl;
    private Double routeDistanceKm;
    private Double routeDurationMinutes;
    private Instant createdAt;
    private Instant updatedAt;
}
