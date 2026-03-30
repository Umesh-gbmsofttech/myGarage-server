package com.gbm.app.dto;

import com.gbm.app.entity.ApprovalStatus;
import com.gbm.app.entity.MechanicRegistrationSource;
import com.gbm.app.entity.Role;
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
    private Role role;
    private ApprovalStatus approvalStatus;
    private MechanicRegistrationSource registrationSource;
    private String certificate;
    private Long garageOwnerUserId;
    private boolean garageOwner;
    private boolean available;
    private String certificationDocumentUrl;
    private String shopActDocumentUrl;
}
