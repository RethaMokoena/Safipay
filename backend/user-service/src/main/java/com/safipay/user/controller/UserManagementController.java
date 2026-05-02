package com.safipay.user.controller;

import com.safipay.user.dto.request.ChangePasswordRequest;
import com.safipay.user.dto.request.RefreshTokenRequest;
import com.safipay.user.dto.response.*;
import com.safipay.user.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserManagementController {

    private final UserAdminService adminService;

    // ── Password & Token ──────────────────────────────────────────

    @PutMapping("/api/auth/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ChangePasswordRequest req) {
        adminService.changePassword(userId, req);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully", null));
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", adminService.refreshToken(req)));
    }

    // ── Admin: User Management ────────────────────────────────────
    // In production, guard these with a ROLE_ADMIN check via @PreAuthorize

    @GetMapping("/api/admin/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(adminService.listUsers(page, size)));
    }

    @GetMapping("/api/admin/users/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUserById(userId)));
    }

    @PostMapping("/api/admin/users/{userId}/suspend")
    public ResponseEntity<ApiResponse<UserResponse>> suspend(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("User suspended", adminService.suspendUser(userId)));
    }

    @PostMapping("/api/admin/users/{userId}/reactivate")
    public ResponseEntity<ApiResponse<UserResponse>> reactivate(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("User reactivated", adminService.reactivateUser(userId)));
    }

    @PostMapping("/api/admin/users/{userId}/promote")
    public ResponseEntity<ApiResponse<UserResponse>> promote(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("User promoted to admin", adminService.promoteToAdmin(userId)));
    }
}
