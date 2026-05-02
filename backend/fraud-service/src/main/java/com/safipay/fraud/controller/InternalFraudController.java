package com.safipay.fraud.controller;

import com.safipay.fraud.dto.request.EvaluateTransactionRequest;
import com.safipay.fraud.dto.response.*;
import com.safipay.fraud.service.FraudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/fraud")
@RequiredArgsConstructor
public class InternalFraudController {

    private final FraudService fraudService;

    /**
     * Called by other services before processing any financial transaction.
     * Returns APPROVED, REVIEW, or BLOCKED.
     */
    @PostMapping("/evaluate")
    public ResponseEntity<ApiResponse<FraudEvaluationResponse>> evaluate(
            @Valid @RequestBody EvaluateTransactionRequest req) {
        FraudEvaluationResponse response = fraudService.evaluateTransaction(req);
        HttpStatus status = switch (response.getDecision()) {
            case BLOCKED -> HttpStatus.FORBIDDEN;
            case REVIEW  -> HttpStatus.ACCEPTED;
            case APPROVED -> HttpStatus.OK;
        };
        return ResponseEntity.status(status).body(ApiResponse.success(response));
    }

    @GetMapping("/users/{userId}/profile")
    public ResponseEntity<ApiResponse<UserRiskProfileResponse>> getProfile(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(fraudService.getUserProfile(userId)));
    }
}
