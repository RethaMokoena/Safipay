package com.safipay.wallet.controller;

import com.safipay.wallet.dto.response.*;
import com.safipay.wallet.exception.WalletException;
import com.safipay.wallet.model.Wallet;
import com.safipay.wallet.repository.WalletRepository;
import com.safipay.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/wallets")
@RequiredArgsConstructor
public class WalletAdminController {

    private final WalletRepository walletRepo;
    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WalletResponse>>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var wallets = walletRepo.findAll(org.springframework.data.domain.PageRequest.of(page, size))
                .stream()
                .map(w -> WalletResponse.builder()
                        .id(w.getId()).userId(w.getUserId())
                        .balance(w.getBalance()).lockedBalance(w.getLockedBalance())
                        .availableBalance(w.getAvailableBalance())
                        .currency(w.getCurrency()).status(w.getStatus())
                        .createdAt(w.getCreatedAt()).build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(wallets));
    }

    @PostMapping("/{userId}/freeze")
    public ResponseEntity<ApiResponse<Void>> freeze(@PathVariable String userId) {
        Wallet wallet = walletRepo.findByUserId(userId)
                .orElseThrow(() -> new WalletException("Wallet not found for user: " + userId));
        wallet.setStatus(Wallet.WalletStatus.FROZEN);
        walletRepo.save(wallet);
        return ResponseEntity.ok(ApiResponse.success("Wallet frozen", null));
    }

    @PostMapping("/{userId}/unfreeze")
    public ResponseEntity<ApiResponse<Void>> unfreeze(@PathVariable String userId) {
        Wallet wallet = walletRepo.findByUserId(userId)
                .orElseThrow(() -> new WalletException("Wallet not found for user: " + userId));
        wallet.setStatus(Wallet.WalletStatus.ACTIVE);
        walletRepo.save(wallet);
        return ResponseEntity.ok(ApiResponse.success("Wallet unfrozen", null));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWallet(userId)));
    }
}
