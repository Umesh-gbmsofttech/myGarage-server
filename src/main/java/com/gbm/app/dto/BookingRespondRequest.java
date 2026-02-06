package com.gbm.app.dto;

import com.gbm.app.entity.BookingStatus;

import lombok.Data;

@Data
public class BookingRespondRequest {
    private BookingStatus status;
}
