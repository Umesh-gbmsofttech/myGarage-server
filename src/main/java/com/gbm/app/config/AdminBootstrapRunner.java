package com.gbm.app.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gbm.app.entity.AdminSettings;
import com.gbm.app.entity.Role;
import com.gbm.app.entity.User;
import com.gbm.app.repository.AdminSettingsRepository;
import com.gbm.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AdminSettingsRepository adminSettingsRepository;
    private final AdminProperties adminProperties;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        ensureAdminUser();
        ensureAdminSettings();
    }

    private void ensureAdminUser() {
        if (adminProperties.getUsername() == null || adminProperties.getUsername().isBlank()
                || adminProperties.getEmail() == null || adminProperties.getEmail().isBlank()
                || adminProperties.getPassword() == null || adminProperties.getPassword().isBlank()) {
            return;
        }

        if (userRepository.findByUsername(adminProperties.getUsername()).isPresent()
                || userRepository.findByEmail(adminProperties.getEmail()).isPresent()) {
            return;
        }

        User admin = new User();
        admin.setUsername(adminProperties.getUsername());
        admin.setEmail(adminProperties.getEmail());
        admin.setFirstName(adminProperties.getName());
        admin.setRole(resolveAdminRole(adminProperties.getRole()));
        admin.setPasswordHash(passwordEncoder.encode(adminProperties.getPassword()));
        userRepository.save(admin);
    }

    private void ensureAdminSettings() {
        if (adminSettingsRepository.findAll().isEmpty()) {
            adminSettingsRepository.save(new AdminSettings());
        }
    }

    private Role resolveAdminRole(String roleProperty) {
        if (roleProperty == null || roleProperty.isBlank()) {
            return Role.ADMIN;
        }
        String normalized = roleProperty.trim().replace('-', '_').replace(' ', '_').toUpperCase();
        if ("SUPER_ADMIN".equals(normalized)) {
            return Role.ADMIN;
        }
        try {
            return Role.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return Role.ADMIN;
        }
    }
}
