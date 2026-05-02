package com.safipay.stokvel.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;

@Component @Slf4j
public class WalletClient {
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${services.wallet-service-url:http://localhost:8082}") private String walletUrl;

    @Data static class WalletRequest {
        private BigDecimal amount; private String referenceId; private String description;
    }

    public void debit(String userId, BigDecimal amount, String refId, String desc) {
        try {
            WalletRequest req = new WalletRequest();
            req.setAmount(amount); req.setReferenceId(refId); req.setDescription(desc);
            restTemplate.postForEntity(walletUrl + "/internal/wallets/" + userId + "/debit", req, Object.class);
        } catch (Exception e) { throw new RuntimeException("Wallet debit failed: " + e.getMessage()); }
    }

    public void credit(String userId, BigDecimal amount, String refId, String desc) {
        try {
            WalletRequest req = new WalletRequest();
            req.setAmount(amount); req.setReferenceId(refId); req.setDescription(desc);
            restTemplate.postForEntity(walletUrl + "/internal/wallets/" + userId + "/credit", req, Object.class);
        } catch (Exception e) { throw new RuntimeException("Wallet credit failed: " + e.getMessage()); }
    }
}
