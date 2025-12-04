package com.safipay.wallet.controller;

import com.safipay.wallet.dto.*;
import com.safipay.wallet.model.Wallet;
import com.safipay.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService service;

    @PostMapping("/create")
    public Wallet create(@RequestBody CreateWalletRequest req) {
        return service.createWallet(req.getUserId());
    }

    @PostMapping("/credit")
    public Wallet credit(@RequestBody CreditRequest req) {
        return service.credit(req.getWalletId(), req.getAmount());
    }

    @PostMapping("/debit")
    public Wallet debit(@RequestBody DebitRequest req) {
        return service.debit(req.getWalletId(), req.getAmount());
    }

    @PostMapping("/transfer")
    public String transfer(@RequestBody TransferRequest req) {
        service.transfer(req.getFromWalletId(), req.getToWalletId(), req.getAmount());
        return "Transfer completed";
    }
}
