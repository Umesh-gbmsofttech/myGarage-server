package com.gbm.app.dto;

import java.util.List;

import com.gbm.app.entity.ApprovalStatus;
import com.gbm.app.entity.MechanicRegistrationSource;
import com.gbm.app.entity.Role;

import lombok.Data;

@Data
public class UserProfileResponse {
    private Long userId;
    private String username;
    private String firstName;
    private String surname;
    private String email;
    private String mobile;
    private Role role;

    private String experience;
    private String speciality;
    private String city;
    private Double rating;
    private Integer ratingCount;
    private boolean visible;
    private String profileImageUrl;
    private String expertise;
    private String about;
    private String certificate;
    private ApprovalStatus approvalStatus;
    private MechanicRegistrationSource registrationSource;
    private Long garageOwnerUserId;
    private Boolean garageOwnerEligible;
    private Boolean available;
    private String certificationDocumentUrl;
    private String shopActDocumentUrl;

    private String addressLine;
    private String avatarUrl;

    private Boolean showAllMechanics;
    private List<MechanicCardResponse> myMechanics;
}
