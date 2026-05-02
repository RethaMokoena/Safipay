package com.safipay.user.service;

import com.safipay.user.dto.request.ChangePasswordRequest;
import com.safipay.user.dto.request.RefreshTokenRequest;
import com.safipay.user.dto.response.ApiResponse;
import com.safipay.user.dto.response.AuthResponse;
import com.safipay.user.dto.response.UserResponse;
import com.safipay.user.exception.InvalidCredentialsException;
import com.safipay.user.exception.UserNotFoundException;
import com.safipay.user.model.User;
import com.safipay.user.repository.UserRepository;
import com.safipay.user.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // ── Password management ───────────────────────────────────────

    public void changePassword(String userId, ChangePasswordRequest req) {
        User user = getUserOrThrow(userId);
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user {}", userId);
    }

    // ── Token refresh ─────────────────────────────────────────────

    public AuthResponse refreshToken(RefreshTokenRequest req) {
        if (!jwtUtil.validateToken(req.getRefreshToken())) {
            throw new InvalidCredentialsException();
        }
        String userId = jwtUtil.extractUserId(req.getRefreshToken());
        User user = getUserOrThrow(userId);

        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(user))
                .refreshToken(jwtUtil.generateRefreshToken(user))
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .user(toUserResponse(user))
                .build();
    }

    // ── Admin: user management ────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size))
                .stream().map(this::toUserResponse).collect(Collectors.toList());
    }

    public UserResponse suspendUser(String userId) {
        User user = getUserOrThrow(userId);
        user.setStatus(User.UserStatus.SUSPENDED);
        log.warn("User {} suspended", userId);
        return toUserResponse(userRepository.save(user));
    }

    public UserResponse reactivateUser(String userId) {
        User user = getUserOrThrow(userId);
        user.setStatus(User.UserStatus.ACTIVE);
        log.info("User {} reactivated", userId);
        return toUserResponse(userRepository.save(user));
    }

    public UserResponse promoteToAdmin(String userId) {
        User user = getUserOrThrow(userId);
        user.setRole(User.Role.ADMIN);
        log.warn("User {} promoted to ADMIN", userId);
        return toUserResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        return toUserResponse(getUserOrThrow(userId));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private User getUserOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId()).email(user.getEmail())
                .firstName(user.getFirstName()).lastName(user.getLastName())
                .fullName(user.getFullName()).phoneNumber(user.getPhoneNumber())
                .role(user.getRole()).status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .profilePictureUrl(user.getProfilePictureUrl())
                .createdAt(user.getCreatedAt()).build();
    }
}
