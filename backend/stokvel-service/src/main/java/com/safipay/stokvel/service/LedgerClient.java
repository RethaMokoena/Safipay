package com.safipay.stokvel.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;

@Component @Slf4j
public class LedgerClient {
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${services.ledger-service-url:http://localhost:8085}") private String ledgerUrl;

    @Data
    static class PostTxRequest {
        private String debitOwnerId; private String creditOwnerId;
        private BigDecimal amount; private String category;
        private String description; private String externalRef; private String initiatedBy;
    }

    public void postContribution(String userId, String stokvelId, BigDecimal amount, String refId) {
        try {
            PostTxRequest req = new PostTxRequest();
            req.setDebitOwnerId(userId); req.setCreditOwnerId("STOKVEL_" + stokvelId);
            req.setAmount(amount); req.setCategory("STOKVEL_CONTRIBUTION");
            req.setDescription("Stokvel contribution"); req.setExternalRef(refId);
            req.setInitiatedBy(userId);
            restTemplate.postForEntity(ledgerUrl + "/internal/ledger/transactions", req, Object.class);
        } catch (Exception e) { log.warn("Ledger unavailable for contribution: {}", e.getMessage()); }
    }

    public void postPayout(String stokvelId, String recipientId, BigDecimal amount, String refId) {
        try {
            PostTxRequest req = new PostTxRequest();
            req.setDebitOwnerId("STOKVEL_" + stokvelId); req.setCreditOwnerId(recipientId);
            req.setAmount(amount); req.setCategory("STOKVEL_PAYOUT");
            req.setDescription("Stokvel payout"); req.setExternalRef(refId);
            restTemplate.postForEntity(ledgerUrl + "/internal/ledger/transactions", req, Object.class);
        } catch (Exception e) { log.warn("Ledger unavailable for payout: {}", e.getMessage()); }
    }
}
