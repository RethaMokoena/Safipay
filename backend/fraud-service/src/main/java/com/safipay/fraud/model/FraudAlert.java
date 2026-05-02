package com.safipay.fraud.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A fraud alert raised when a transaction scores above the HIGH threshold
 * or matches a known fraud pattern. Alerts require manual review or auto-block.
 */
@Entity
@Table(name = "fraud_alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FraudAlert {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id")
    private FraudEvaluation evaluation;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    @Builder.Default
    private AlertStatus status = AlertStatus.OPEN;

    @Column(length = 1000)
    private String description;

    // Reviewer who resolved the alert
    @Column private String resolvedBy;
    @Column(length = 500) private String resolutionNotes;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;

    public enum AlertType {
        VELOCITY_BREACH,       // Too many transactions too fast
        LARGE_AMOUNT,          // Unusually large transaction
        UNUSUAL_PATTERN,       // Behaviour differs from user baseline
        BLACKLISTED_RECIPIENT, // Sending to a flagged account
        MULTIPLE_FAILURES,     // Repeated failed transactions
        DEVICE_ANOMALY,        // New/unknown device
        GEOGRAPHIC_ANOMALY,    // IP from unusual location
        ROUND_TRIPPING,        // Money sent and returned rapidly
        ACCOUNT_TAKEOVER       // Signs of compromised account
    }

    public enum AlertStatus { OPEN, UNDER_REVIEW, RESOLVED_FRAUD, RESOLVED_LEGITIMATE, DISMISSED }
}
