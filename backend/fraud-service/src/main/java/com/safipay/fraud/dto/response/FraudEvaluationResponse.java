package com.safipay.fraud.dto.response;
import com.safipay.fraud.model.FraudEvaluation;
import lombok.*; import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.List;

@Data @Builder
public class FraudEvaluationResponse {
    private String id; private String transactionRef;
    private String userId; private BigDecimal amount;
    private FraudEvaluation.TransactionType transactionType;
    private Integer riskScore; private FraudEvaluation.RiskLevel riskLevel;
    private FraudEvaluation.FraudDecision decision;
    private List<String> triggeredRules; private String decisionReason;
    private LocalDateTime evaluatedAt;
}
