package com.safipay.payment.controller;

import com.safipay.payment.dto.request.*;
import com.safipay.payment.dto.response.*;
import com.safipay.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<PaymentResponse>> send(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody SendMoneyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment sent", paymentService.sendMoney(userId, req)));
    }

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<PaymentResponse>> request(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody RequestMoneyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Request created", paymentService.requestMoney(userId, req)));
    }

    @PostMapping("/requests/{paymentId}/approve")
    public ResponseEntity<ApiResponse<PaymentResponse>> approve(
            @PathVariable String paymentId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("Request approved", paymentService.approveRequest(paymentId, userId)));
    }

    @PostMapping("/requests/{paymentId}/decline")
    public ResponseEntity<ApiResponse<PaymentResponse>> decline(
            @PathVariable String paymentId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("Request declined", paymentService.declineRequest(paymentId, userId)));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @PathVariable String paymentId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success("Refund processed", paymentService.refund(paymentId, userId)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> history(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getHistory(userId, page, size)));
    }

    @GetMapping("/pending-requests")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> pendingRequests(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPendingRequests(userId)));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable String paymentId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getById(paymentId)));
    }
}
