package com.safipay.merchant.controller;

import com.safipay.merchant.dto.request.*;
import com.safipay.merchant.dto.response.*;
import com.safipay.merchant.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    // ── Merchant CRUD ─────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ApiResponse<MerchantResponse>> register(
            @Valid @RequestBody RegisterMerchantRequest req,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Merchant registered, pending verification",
                        merchantService.registerMerchant(req, userId)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MerchantResponse>>> getMyMerchants(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(merchantService.getMyMerchants(userId)));
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<ApiResponse<MerchantResponse>> getMerchant(
            @PathVariable String merchantId) {
        return ResponseEntity.ok(ApiResponse.success(merchantService.getMerchant(merchantId)));
    }

    // Admin endpoints
    @PostMapping("/{merchantId}/approve")
    public ResponseEntity<ApiResponse<MerchantResponse>> approve(@PathVariable String merchantId) {
        return ResponseEntity.ok(ApiResponse.success("Merchant approved", merchantService.approveMerchant(merchantId)));
    }

    @PostMapping("/{merchantId}/suspend")
    public ResponseEntity<ApiResponse<MerchantResponse>> suspend(@PathVariable String merchantId) {
        return ResponseEntity.ok(ApiResponse.success("Merchant suspended", merchantService.suspendMerchant(merchantId)));
    }

    // ── API Keys ──────────────────────────────────────────────────

    @PostMapping("/{merchantId}/api-keys")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> generateKey(
            @PathVariable String merchantId,
            @Valid @RequestBody CreateApiKeyRequest req,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("API key created — save it now, it won't be shown again",
                        merchantService.generateApiKey(merchantId, req, userId)));
    }

    @GetMapping("/{merchantId}/api-keys")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> listKeys(
            @PathVariable String merchantId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(merchantService.listApiKeys(merchantId, userId)));
    }

    @DeleteMapping("/{merchantId}/api-keys/{keyId}")
    public ResponseEntity<ApiResponse<Void>> revokeKey(
            @PathVariable String merchantId,
            @PathVariable String keyId,
            @AuthenticationPrincipal String userId) {
        merchantService.revokeApiKey(keyId, merchantId, userId);
        return ResponseEntity.ok(ApiResponse.success("API key revoked", null));
    }

    // ── Payments ──────────────────────────────────────────────────

    @PostMapping("/{merchantId}/payments/charge")
    public ResponseEntity<ApiResponse<MerchantPaymentResponse>> charge(
            @PathVariable String merchantId,
            @Valid @RequestBody MerchantPaymentRequest req,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment successful",
                        merchantService.chargeCustomer(merchantId, req, userId)));
    }

    @PostMapping("/{merchantId}/payments/{paymentId}/refund")
    public ResponseEntity<ApiResponse<MerchantPaymentResponse>> refund(
            @PathVariable String merchantId,
            @PathVariable String paymentId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("Refund processed",
                merchantService.refundPayment(paymentId, merchantId, userId)));
    }

    @GetMapping("/{merchantId}/payments")
    public ResponseEntity<ApiResponse<List<MerchantPaymentResponse>>> getPayments(
            @PathVariable String merchantId,
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                merchantService.getPayments(merchantId, userId, page, size)));
    }
}
