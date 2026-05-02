package com.safipay.stokvel.controller;

import com.safipay.stokvel.dto.request.*;
import com.safipay.stokvel.dto.response.*;
import com.safipay.stokvel.service.StokvelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/stokvels") @RequiredArgsConstructor
public class StokvelController {
    private final StokvelService svc;

    @PostMapping
    public ResponseEntity<ApiResponse<StokvelResponse>> create(@Valid @RequestBody CreateStokvelRequest req, @AuthenticationPrincipal String userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Created", svc.create(req, userId)));
    }
    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<StokvelResponse>> join(@PathVariable Long id, @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("Joined", svc.join(id, userId)));
    }
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<StokvelResponse>> activate(@PathVariable Long id, @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("Activated", svc.activate(id, userId)));
    }
    @PostMapping("/{id}/contribute")
    public ResponseEntity<ApiResponse<ContributionResponse>> contribute(@PathVariable Long id, @AuthenticationPrincipal String userId, @Valid @RequestBody ContributeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Contributed", svc.contribute(id, userId, req)));
    }
    @PostMapping("/{id}/payouts/rosca")
    public ResponseEntity<ApiResponse<PayoutResponse>> roscaPayout(@PathVariable Long id, @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("Payout processed", svc.triggerRosca(id, userId)));
    }
    @PostMapping("/{id}/payouts/pool/{recipientUserId}")
    public ResponseEntity<ApiResponse<PayoutResponse>> poolWithdraw(@PathVariable Long id, @PathVariable String recipientUserId, @AuthenticationPrincipal String userId, @Valid @RequestBody PoolWithdrawalRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Withdrawal processed", svc.poolWithdraw(id, recipientUserId, req, userId)));
    }
    @GetMapping
    public ResponseEntity<ApiResponse<List<StokvelResponse>>> getAll() { return ResponseEntity.ok(ApiResponse.success(svc.getAll())); }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StokvelResponse>> getById(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success(svc.getById(id))); }
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<StokvelResponse>>> getMine(@AuthenticationPrincipal String userId) { return ResponseEntity.ok(ApiResponse.success(svc.getMine(userId))); }
    @GetMapping("/{id}/contributions")
    public ResponseEntity<ApiResponse<List<ContributionResponse>>> getContributions(@PathVariable Long id, @AuthenticationPrincipal String userId) { return ResponseEntity.ok(ApiResponse.success(svc.getContributions(id, userId))); }
    @GetMapping("/{id}/payouts")
    public ResponseEntity<ApiResponse<List<PayoutResponse>>> getPayouts(@PathVariable Long id, @AuthenticationPrincipal String userId) { return ResponseEntity.ok(ApiResponse.success(svc.getPayouts(id, userId))); }
}
