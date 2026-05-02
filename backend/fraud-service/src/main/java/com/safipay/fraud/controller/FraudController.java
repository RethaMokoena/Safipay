package com.safipay.fraud.controller;

import com.safipay.fraud.dto.request.*;
import com.safipay.fraud.dto.response.*;
import com.safipay.fraud.service.FraudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudService fraudService;

    // ── My risk profile ───────────────────────────────────────────
    @GetMapping("/profile/me")
    public ResponseEntity<ApiResponse<UserRiskProfileResponse>> getMyProfile(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(fraudService.getUserProfile(userId)));
    }

    @GetMapping("/history/me")
    public ResponseEntity<ApiResponse<List<FraudEvaluationResponse>>> getMyHistory(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(fraudService.getUserHistory(userId, page, size)));
    }

    // ── Admin endpoints ───────────────────────────────────────────
    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<FraudAlertResponse>>> getOpenAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(fraudService.getOpenAlerts(page, size)));
    }

    @PostMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> resolveAlert(
            @PathVariable String alertId,
            @Valid @RequestBody ResolveAlertRequest req,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("Alert resolved",
                fraudService.resolveAlert(alertId, req, userId)));
    }

    @PostMapping("/users/{userId}/blacklist")
    public ResponseEntity<ApiResponse<UserRiskProfileResponse>> blacklist(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("User blacklisted",
                fraudService.blacklistUser(userId)));
    }

    @PostMapping("/users/{userId}/unblacklist")
    public ResponseEntity<ApiResponse<UserRiskProfileResponse>> unblacklist(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("User removed from blacklist",
                fraudService.unblacklistUser(userId)));
    }

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<ApiResponse<UserRiskProfileResponse>> getUserProfile(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(fraudService.getUserProfile(userId)));
    }
}
