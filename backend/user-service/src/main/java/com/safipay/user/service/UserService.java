package com.safipay.user.service;

import com.safipay.user.dto.request.*;
import com.safipay.user.dto.response.*;
import com.safipay.user.exception.*;
import com.safipay.user.model.User;
import com.safipay.user.repository.UserRepository;
import com.safipay.user.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Slf4j @Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber()))
            throw new UserAlreadyExistsException("Phone number already registered");

        User user = User.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .phoneNumber(request.getPhoneNumber())
            .build();

        user = userRepository.save(user);
        log.info("Registered new user: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new InvalidCredentialsException();

        if (user.getStatus() == User.UserStatus.SUSPENDED)
            throw new InvalidCredentialsException();

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String userId) {
        return toUserResponse(getUserOrThrow(userId));
    }

    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = getUserOrThrow(userId);
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getProfilePictureUrl() != null) user.setProfilePictureUrl(request.getProfilePictureUrl());
        return toUserResponse(userRepository.save(user));
    }

    // Called by other services (via internal/feign) to validate a token and return user info
    @Transactional(readOnly = true)
    public UserResponse validateAndGetUser(String userId) {
        return toUserResponse(getUserOrThrow(userId));
    }

    private User getUserOrThrow(String id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
            .accessToken(jwtUtil.generateAccessToken(user))
            .refreshToken(jwtUtil.generateRefreshToken(user))
            .tokenType("Bearer")
            .expiresIn(86400000L)
            .user(toUserResponse(user))
            .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .fullName(user.getFullName())
            .phoneNumber(user.getPhoneNumber())
            .role(user.getRole())
            .status(user.getStatus())
            .emailVerified(user.getEmailVerified())
            .profilePictureUrl(user.getProfilePictureUrl())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
// NOTE: The following methods are appended — merge with the class body manually if needed
