package com.gbm.app.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String surname;
    private String mobile;
    private String city;
    private String experience;
    private String speciality;
    private Boolean shopActive;
    private String profileImageUrl;
    private String expertise;
    private String about;
    private String addressLine;
    private String avatarUrl;
}
