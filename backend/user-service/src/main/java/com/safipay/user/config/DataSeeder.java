package com.safipay.user.config;

import com.safipay.user.model.User;
import com.safipay.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@safipay.co.za}")
    private String adminEmail;

    @Value("${admin.password:Admin@123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user already exists: {}", adminEmail);
            return;
        }

        User admin = User.builder()
                .firstName("SafiPay")
                .lastName("Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(User.Role.ADMIN)
                .status(User.UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        userRepository.save(admin);

        log.warn("========================================");
        log.warn("  Default admin user created:");
        log.warn("  Email:    {}", adminEmail);
        log.warn("  Password: {}", adminPassword);
        log.warn("  CHANGE THIS PASSWORD IMMEDIATELY");
        log.warn("========================================");
    }
}