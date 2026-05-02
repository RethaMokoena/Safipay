package com.safipay.ledger.controller;

import com.safipay.ledger.dto.request.*;
import com.safipay.ledger.dto.response.*;
import com.safipay.ledger.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    // Create ledger account (called at user registration)
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created", ledgerService.createAccount(req)));
    }

    // Get my ledger account
    @GetMapping("/accounts/me")
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> getMyAccount(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.success(ledgerService.getAccount(userId)));
    }

    // Get any account by ownerId (admin / internal use)
    @GetMapping("/accounts/{ownerId}")
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> getAccount(
            @PathVariable String ownerId) {
        return ResponseEntity.ok(ApiResponse.success(ledgerService.getAccount(ownerId)));
    }

    // My ledger entries (paginated)
    @GetMapping("/entries")
    public ResponseEntity<ApiResponse<List<LedgerEntryResponse>>> getMyEntries(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(ledgerService.getEntries(userId, page, size)));
    }

    // Account statement for a date range
    @GetMapping("/statement")
    public ResponseEntity<ApiResponse<AccountStatementResponse>> getStatement(
            @AuthenticationPrincipal String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.success(ledgerService.getStatement(userId, from, to)));
    }

    // Reverse a transaction (admin only)
    @PostMapping("/transactions/{txId}/reverse")
    public ResponseEntity<ApiResponse<LedgerTransactionResponse>> reverse(
            @PathVariable String txId) {
        return ResponseEntity.ok(ApiResponse.success("Reversed", ledgerService.reverseTransaction(txId)));
    }
}
