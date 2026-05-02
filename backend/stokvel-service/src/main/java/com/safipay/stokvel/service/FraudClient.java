package com.safipay.stokvel.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;

@Component @Slf4j
public class FraudClient {
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${services.fraud-service-url:http://localhost:8087}") private String fraudUrl;

    @Data static class EvalRequest {
        private String transactionRef; private String userId;
        private BigDecimal amount; private String transactionType;
    }
    @Data static class EvalResponse {
        private boolean success; private EvalData data;
        @Data static class EvalData { private String decision; }
    }

    public String evaluate(String ref, String userId, BigDecimal amount, String type) {
        try {
            EvalRequest req = new EvalRequest();
            req.setTransactionRef(ref); req.setUserId(userId);
            req.setAmount(amount); req.setTransactionType(type);
            var resp = restTemplate.postForEntity(fraudUrl + "/internal/fraud/evaluate", req, EvalResponse.class);
            if (resp.getBody() != null && resp.getBody().getData() != null)
                return resp.getBody().getData().getDecision();
        } catch (Exception e) { log.warn("Fraud service unavailable: {}", e.getMessage()); }
        return "APPROVED";
    }
}
