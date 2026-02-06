package com.gbm.app.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.gbm.app.entity.AdminSettings;
import com.gbm.app.entity.Role;
import com.gbm.app.entity.User;
import com.gbm.app.repository.AdminSettingsRepository;
import com.gbm.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AdminSettingsRepository adminSettingsRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("MyGarageAdmin").isEmpty()) {
            User admin = new User();
            admin.setUsername("MyGarageAdmin");
            admin.setEmail("admin@mygarage.local");
            admin.setFirstName("Admin");
            admin.setSurname("User");
            admin.setMobile("0000000000");
            admin.setRole(Role.ADMIN);
            admin.setPasswordHash(passwordEncoder.encode("MyGaragePassword"));
            userRepository.save(admin);
        }

        if (adminSettingsRepository.findAll().isEmpty()) {
            adminSettingsRepository.save(new AdminSettings());
        }
    }
}
