package com.gbm.app.dto;

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
    private boolean shopActive;
    private Double rating;
    private Integer ratingCount;
    private boolean visible;
    private String profileImageUrl;
    private String expertise;
    private String about;

    private String addressLine;
    private String avatarUrl;

    private Boolean showAllMechanics;
}
