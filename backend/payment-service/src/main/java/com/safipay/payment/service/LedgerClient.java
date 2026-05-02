package com.safipay.payment.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
@Slf4j
public class LedgerClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.ledger-service-url:http://localhost:8085}")
    private String ledgerUrl;

    @Data
    static class PostTxRequest {
        private String debitOwnerId;
        private String creditOwnerId;
        private BigDecimal amount;
        private String category;
        private String description;
        private String externalRef;
        private String initiatedBy;
    }

    public void postTransfer(String debitUserId, String creditUserId,
                              BigDecimal amount, String paymentId, String description) {
        try {
            PostTxRequest req = new PostTxRequest();
            req.setDebitOwnerId(debitUserId);
            req.setCreditOwnerId(creditUserId);
            req.setAmount(amount);
            req.setCategory("TRANSFER");
            req.setDescription(description);
            req.setExternalRef(paymentId);
            req.setInitiatedBy(debitUserId);
            restTemplate.postForEntity(ledgerUrl + "/internal/ledger/transactions", req, Object.class);
        } catch (Exception e) {
            log.warn("Ledger service unavailable, skipping ledger entry: {}", e.getMessage());
        }
    }

    public void postReversal(String debitUserId, String creditUserId,
                              BigDecimal amount, String originalPaymentId) {
        try {
            PostTxRequest req = new PostTxRequest();
            req.setDebitOwnerId(debitUserId);
            req.setCreditOwnerId(creditUserId);
            req.setAmount(amount);
            req.setCategory("REVERSAL");
            req.setDescription("Refund for payment: " + originalPaymentId);
            req.setExternalRef("REV-" + originalPaymentId);
            restTemplate.postForEntity(ledgerUrl + "/internal/ledger/transactions", req, Object.class);
        } catch (Exception e) {
            log.warn("Ledger service unavailable for reversal: {}", e.getMessage());
        }
    }
}
