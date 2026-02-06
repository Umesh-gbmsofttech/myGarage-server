package com.gbm.app.dto;

import com.gbm.app.entity.OtpType;

import lombok.Data;

@Data
public class OtpVerifyRequest {
    private OtpType type;
    private String code;
}
