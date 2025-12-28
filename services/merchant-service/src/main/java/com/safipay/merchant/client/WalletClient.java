package com.safipay.merchant.client;

import com.safipay.merchant.dto.WalletBalanceResponse;
import com.safipay.merchant.dto.WalletCreateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(name = "wallet-service", url = "${clients.wallet.url:http://localhost:8081}")
public interface WalletClient {

    @PostMapping("/wallet/create")
    WalletCreateResponse createWallet();

    @GetMapping("/wallet/{walletId}/balance")
    WalletBalanceResponse getBalance(@PathVariable("walletId") UUID walletId);
}
