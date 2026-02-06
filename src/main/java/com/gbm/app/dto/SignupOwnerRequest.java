package com.gbm.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignupOwnerRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String surname;
    @NotBlank
    private String mobile;
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    private String role;
}
