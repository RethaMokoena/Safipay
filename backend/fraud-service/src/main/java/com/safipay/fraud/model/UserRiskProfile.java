package com.safipay.fraud.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Continuously updated behavioural baseline for each user.
 * Used to detect anomalies relative to the user's own history.
 */
@Entity
@Table(name = "user_risk_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRiskProfile {

    @Id
    private String userId;  // matches user-service userId

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default
    private RiskTier riskTier = RiskTier.LOW;

    // Running averages
    @Column(precision = 19, scale = 2) @Builder.Default
    private BigDecimal avgTransactionAmount = BigDecimal.ZERO;

    @Column @Builder.Default
    private Integer totalTransactions = 0;

    @Column(precision = 19, scale = 2) @Builder.Default
    private BigDecimal totalTransactionVolume = BigDecimal.ZERO;

    // Velocity counters (reset periodically)
    @Column @Builder.Default
    private Integer transactionsLastHour = 0;

    @Column(precision = 19, scale = 2) @Builder.Default
    private BigDecimal amountLastHour = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2) @Builder.Default
    private BigDecimal amountLastDay = BigDecimal.ZERO;

    // Flags
    @Column @Builder.Default private Boolean isBlacklisted = false;
    @Column @Builder.Default private Integer failedTransactionCount = 0;
    @Column @Builder.Default private Integer openAlertCount = 0;

    @Column private String lastKnownDeviceId;
    @Column private String lastKnownIpAddress;
    @Column private LocalDateTime lastTransactionAt;

    @UpdateTimestamp private LocalDateTime updatedAt;

    public enum RiskTier { LOW, MEDIUM, HIGH, BLACKLISTED }
}
