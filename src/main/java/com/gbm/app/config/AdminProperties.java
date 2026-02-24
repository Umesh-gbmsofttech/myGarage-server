package com.gbm.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.admin")
public class AdminProperties {
    private String role;
    private String username;
    private String name;
    private String email;
    private String password;
}
