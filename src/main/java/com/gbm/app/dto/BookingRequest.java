package com.gbm.app.dto;

import lombok.Data;

@Data
public class BookingRequest {
    private Long mechanicId;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleYear;
    private String issueDescription;
}
