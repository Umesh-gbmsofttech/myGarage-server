package com.gbm.app.dto;

import lombok.Data;

@Data
public class MechanicCardResponse {
    private Long mechanicId;
    private String mechName;
    private String surname;
    private String speciality;
    private String expertise;
    private Double rating;
    private Integer ratingCount;
    private String city;
    private String profileImageUrl;
    private Double distanceKm;
}
