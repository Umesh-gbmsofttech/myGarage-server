package com.gbm.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SignupMechanicRequest {
    @NotBlank
    private String mechName;
    @NotBlank
    private String surname;
    @NotBlank
    private String mobile;
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String experience;
    @NotBlank
    private String speciality;
    @NotBlank
    private String city;
    @NotNull
    private Boolean shopAct;
    private Boolean shopActive;
    private String role;
}
