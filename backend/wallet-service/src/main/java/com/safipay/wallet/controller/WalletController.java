package com.safipay.wallet.controller;

import com.safipay.wallet.dto.request.*;
import com.safipay.wallet.dto.response.*;
import com.safipay.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/wallets") @RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<ApiResponse<WalletResponse>> create(@AuthenticationPrincipal String userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Wallet created", walletService.createWallet(userId)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWallet(userId)));
    }

    @PostMapping("/top-up")
    public ResponseEntity<ApiResponse<WalletResponse>> topUp(
            @AuthenticationPrincipal String userId, @Valid @RequestBody TopUpRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Top up successful", walletService.topUp(userId, req)));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<WalletResponse>> transfer(
            @AuthenticationPrincipal String userId, @Valid @RequestBody TransferRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Transfer successful", walletService.transfer(userId, req)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getTransactions(userId, page, size)));
    }
}
