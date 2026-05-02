package com.safipay.payment.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
@Slf4j
public class FraudClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.fraud-service-url:http://localhost:8087}")
    private String fraudUrl;

    @Data
    public static class EvaluateRequest {
        private String transactionRef;
        private String userId;
        private BigDecimal amount;
        private String transactionType;
        private String recipientUserId;
        private String ipAddress;
    }

    @Data
    public static class EvaluationResult {
        private String decision;  // APPROVED, REVIEW, BLOCKED
        private Integer riskScore;
        private String decisionReason;
    }

    /**
     * Returns APPROVED, REVIEW, or BLOCKED.
     * Falls back to APPROVED if fraud service is unavailable (fail-open for MVP).
     */
    public String evaluate(String transactionRef, String userId, BigDecimal amount,
                           String type, String recipientUserId) {
        try {
            EvaluateRequest req = new EvaluateRequest();
            req.setTransactionRef(transactionRef);
            req.setUserId(userId);
            req.setAmount(amount);
            req.setTransactionType(type);
            req.setRecipientUserId(recipientUserId);

            var response = restTemplate.postForEntity(
                    fraudUrl + "/internal/fraud/evaluate", req, FraudEvaluationResponse.class);

            if (response.getBody() != null && response.getBody().getData() != null) {
                return response.getBody().getData().getDecision();
            }
        } catch (Exception e) {
            log.warn("Fraud service unavailable, failing open: {}", e.getMessage());
        }
        return "APPROVED";
    }

    @Data
    private static class FraudEvaluationResponse {
        private boolean success;
        private FraudData data;

        @Data
        static class FraudData {
            private String decision;
            private Integer riskScore;
        }
    }
}
