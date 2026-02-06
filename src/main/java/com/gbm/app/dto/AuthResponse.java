package com.gbm.app.dto;

import com.gbm.app.entity.Role;

import lombok.Data;

@Data
public class AuthResponse {
    private Long userId;
    private String token;
    private Role role;
    private String name;
    private String firstName;
    private String surname;
    private String email;
}
