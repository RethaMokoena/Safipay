package com.safipay.wallet.controller;

import com.safipay.wallet.dto.response.*;
import com.safipay.wallet.service.WalletService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController @RequestMapping("/internal/wallets") @RequiredArgsConstructor
public class InternalWalletController {
    private final WalletService walletService;

    @PostMapping("/{userId}/create")
    public ResponseEntity<ApiResponse<WalletResponse>> create(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.createWallet(userId)));
    }

    @PostMapping("/{userId}/credit")
    public ResponseEntity<ApiResponse<Void>> credit(@PathVariable String userId, @RequestBody CreditDebitRequest req) {
        walletService.internalCredit(userId, req.getAmount(), req.getReferenceId(), req.getDescription());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{userId}/debit")
    public ResponseEntity<ApiResponse<Void>> debit(@PathVariable String userId, @RequestBody CreditDebitRequest req) {
        walletService.internalDebit(userId, req.getAmount(), req.getReferenceId(), req.getDescription());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Data
    public static class CreditDebitRequest {
        private BigDecimal amount;
        private String referenceId;
        private String description;
    }
}
