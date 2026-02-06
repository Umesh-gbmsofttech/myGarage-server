package com.gbm.app.dto;

import java.time.Instant;

import com.gbm.app.entity.BookingStatus;

import lombok.Data;

@Data
public class BookingResponse {
    private Long id;
    private Long ownerId;
    private Long mechanicId;
    private BookingStatus status;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleYear;
    private String issueDescription;
    private String meetOtp;
    private String completeOtp;
    private boolean meetVerified;
    private boolean completeVerified;
    private Instant createdAt;
    private Instant updatedAt;
}
