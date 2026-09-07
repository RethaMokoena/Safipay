package com.safipay.wallet.controller;

import com.safipay.wallet.dto.request.*;
import com.safipay.wallet.dto.response.*;
import com.safipay.wallet.model.Wallet;
import com.safipay.wallet.repository.WalletRepository;
import com.safipay.wallet.service.WalletService;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/internal/wallets")
@RequiredArgsConstructor
public class InternalWalletController {

    private final WalletService walletService;
    private final WalletRepository walletRepository;

    @PostMapping("/{userId}/create")
    public WalletResponse createWallet(
            @PathVariable String userId,
            @Valid @RequestBody CreateWalletRequest request
    ) {
        return walletService.createWalletIfAbsent(
                userId,
                request.getEmail()
        );
    }

    @GetMapping("/by-email")
    public ResponseEntity<Wallet> getWalletByEmail(
            @RequestParam String email
    ) {
        Wallet wallet = walletRepository
                .findByUserEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Wallet not found for email: " + email
                        )
                );

        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{userId}/credit")
    public ResponseEntity<ApiResponse<Void>> credit(
            @PathVariable String userId,
            @RequestBody CreditDebitRequest req
    ) {
        walletService.internalCredit(
                userId,
                req.getAmount(),
                req.getReferenceId(),
                req.getDescription()
        );

        return ResponseEntity.ok(
                ApiResponse.success(null)
        );
    }

    @PostMapping("/{userId}/debit")
    public ResponseEntity<ApiResponse<Void>> debit(
            @PathVariable String userId,
            @RequestBody CreditDebitRequest req
    ) {
        walletService.internalDebit(
                userId,
                req.getAmount(),
                req.getReferenceId(),
                req.getDescription()
        );

        return ResponseEntity.ok(
                ApiResponse.success(null)
        );
    }

    @Data
    public static class CreditDebitRequest {
        private BigDecimal amount;
        private String referenceId;
        private String description;
    }
}