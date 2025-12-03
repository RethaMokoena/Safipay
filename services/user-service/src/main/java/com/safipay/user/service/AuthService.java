package com.safipay.user.service;

import com.safipay.user.dto.RegisterRequest;
import com.safipay.user.dto.LoginRequest;
import com.safipay.user.model.User;
import com.safipay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public User register(RegisterRequest req) {
        if (repo.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
        if (repo.findByUsername(req.getUsername()).isPresent()) {
            throw new RuntimeException("Username already registered");
        }
        if (repo.findByNationalId(req.getNationalId()).isPresent()) {
            throw new RuntimeException("National ID already registered");
        }
        if (repo.findByPhoneNumber(req.getPhoneNumber()).isPresent()) {  // Optional: add this too
            throw new RuntimeException("Phone number already registered");
        }

        User user = User.builder()
                .username(req.getUsername())      // Add this line
                .email(req.getEmail())
                .password(encoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .phoneNumber(req.getPhoneNumber())
                .nationalId(req.getNationalId())
                .build();

        return repo.save(user);
    }

    public User login(LoginRequest req) {
        User user = repo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return user;
    }
}