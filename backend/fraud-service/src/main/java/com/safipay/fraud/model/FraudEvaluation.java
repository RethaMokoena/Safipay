package com.safipay.fraud.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The result of a real-time fraud risk assessment for a transaction.
 * Stores the score, decision, and all triggered rules for audit.
 */
@Entity
@Table(name = "fraud_evaluations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FraudEvaluation {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // External transaction reference from payment/stokvel/merchant service
    @Column(nullable = false)
    private String transactionRef;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TransactionType transactionType;

    // Composite risk score 0–100 (higher = riskier)
    @Column(nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private FraudDecision decision;

    // Comma-separated list of triggered rule names
    @Column(length = 1000)
    private String triggeredRules;

    @Column(length = 500)
    private String decisionReason;

    // Contextual signals
    @Column private String ipAddress;
    @Column private String deviceId;
    @Column private String recipientUserId;
    @Column private String merchantId;

    @CreationTimestamp
    private LocalDateTime evaluatedAt;

    public enum TransactionType {
        PEER_TRANSFER, TOP_UP, WITHDRAWAL, MERCHANT_PAYMENT,
        STOKVEL_CONTRIBUTION, STOKVEL_PAYOUT
    }

    public enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

    public enum FraudDecision {
        APPROVED,   // proceed normally
        REVIEW,     // flag for manual review but allow
        BLOCKED     // halt the transaction
    }
}
