package com.gbm.app.dto;

import com.gbm.app.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BookingSummaryDTO {
    private Long id;
    private BookingStatus status;
    private String vehicleMake;
    private String vehicleModel;
}
