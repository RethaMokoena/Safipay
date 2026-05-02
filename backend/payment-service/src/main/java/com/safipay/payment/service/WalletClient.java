package com.safipay.payment.service;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;

@Component
public class WalletClient {
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${services.wallet-service-url}") private String walletUrl;

    @Data
    static class WalletRequest { private BigDecimal amount; private String referenceId; private String description; }

    public void debit(String userId, BigDecimal amount, String refId, String desc) {
        WalletRequest req = new WalletRequest();
        req.setAmount(amount); req.setReferenceId(refId); req.setDescription(desc);
        restTemplate.postForEntity(walletUrl + "/internal/wallets/" + userId + "/debit", req, Object.class);
    }

    public void credit(String userId, BigDecimal amount, String refId, String desc) {
        WalletRequest req = new WalletRequest();
        req.setAmount(amount); req.setReferenceId(refId); req.setDescription(desc);
        restTemplate.postForEntity(walletUrl + "/internal/wallets/" + userId + "/credit", req, Object.class);
    }
}
