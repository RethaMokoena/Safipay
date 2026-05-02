package com.safipay.ledger.controller;

import com.safipay.ledger.dto.request.*;
import com.safipay.ledger.dto.response.*;
import com.safipay.ledger.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/ledger")
@RequiredArgsConstructor
public class InternalLedgerController {

    private final LedgerService ledgerService;

    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ledgerService.createAccount(req)));
    }

    @PostMapping("/transactions")
    public ResponseEntity<ApiResponse<LedgerTransactionResponse>> postTransaction(
            @Valid @RequestBody PostTransactionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Posted", ledgerService.postTransaction(req)));
    }

    @GetMapping("/accounts/{ownerId}")
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> getAccount(
            @PathVariable String ownerId) {
        return ResponseEntity.ok(ApiResponse.success(ledgerService.getAccount(ownerId)));
    }
}
